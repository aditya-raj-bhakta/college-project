# Shelfwise: Library Management System

A full-stack library management system for a college library. It tracks every physical copy by barcode, runs a circulation desk for librarians, queues members for books that are out, keeps a ledger of fines, sends due-date reminders, and records every action in an audit log.

**Backend:** Java 17 · Spring Boot 3 · Spring Security (JWT) · Spring Data JPA · Flyway · H2 / MySQL
**Frontend:** React 18 · Vite · React Router · Axios

---

## Features

### Members
- Browse the catalogue with live availability ("2 of 3 on the shelf", "All out · 2 waiting")
- Borrow online: the system picks a free copy, or the copy set aside for them
- Join the holds queue when every copy is out, see their place in line, and get notified when it's their turn
- Renew loans online (within the member type's limit, and only if nobody is waiting)
- See fines with every payment and waiver, and exactly why they can't borrow if they're blocked
- Reminders before a book is due and when it's overdue

Members **cannot return books themselves**: books come back to the circulation desk.

### Librarians
- **Desk:** scan a library card to see the member's standing, scan copies to issue them; scan returns to check them in. Each check-in says where the copy goes next: back to its shelf, or onto the hold shelf for the next member in line.
- **Catalogue:** titles and their physical copies, each with a barcode, shelf and status (on the shelf, on loan, hold shelf, damaged, lost, withdrawn)
- **Loans:** everything out or returned; check in, renew or mark lost
- **Holds:** the hold shelf and the waiting queue across the library
- **Fines:** take payments (full or partial) and waive fines with a reason
- **Members:** search, change member type, suspend and reactivate
- **Reports:** circulation per day, most borrowed titles, fines collected; CSV downloads of loans, fines and overdue books

### Admins
Everything a librarian can do, plus:
- **Rules:** loan limits, loan periods, renewals, fine rates and blocking thresholds per member type; hold pickup window, lost item fee, reminder timing
- **Staff:** create librarian and admin accounts
- **Audit log:** every circulation, fine, catalogue and rules change, with who made it
- Run the daily job on demand

### Library rules (all enforced in the backend)
| Rule | Where |
|------|-------|
| A member can borrow if: active (not suspended), under their loan limit, no overdue books, unpaid fines at or under their type's threshold | `BorrowingPolicy` |
| One copy of a title per member at a time | `CirculationService` |
| Holds only when no copy is on the shelf; a returned copy goes to the first member in line and waits on the hold shelf for N days | `HoldService` |
| Renewals: within the type's limit, not when overdue, not when someone is waiting | `CirculationService.renewBlockedReason` |
| Late fine = days late × the member type's daily rate, charged at check-in | `FineService` |
| Lost copy = lost item fee + any late fine | `CirculationService.markLost` |
| Payments and waivers can't exceed what's owed; waivers need a reason | `Fine`, `WaiverRequest` |
| Only librarians and admins check books in | `@StaffOnly` + URL rules |

Default rules (editable on the Rules page):

| Type | Books | Days | Renewals | Fine/day | Blocked over | Holds |
|------|------:|-----:|---------:|---------:|-------------:|------:|
| Student | 3 | 14 | 1 | ₹10 | ₹100 | 3 |
| Faculty | 10 | 60 | 3 | ₹5 | ₹500 | 10 |
| Staff | 5 | 30 | 2 | ₹5 | ₹200 | 5 |

---

## Architecture

```
React (Vite)  ──/api──▶  Spring Boot REST API  ──JPA──▶  H2 file DB (or MySQL)
                          JWT auth · role checks          schema owned by Flyway
```

The backend is organised **by feature**. Each package owns its entities, repositories, service and controller:

| Package | Responsibility |
|---------|----------------|
| `catalog` | Titles (`Book`) and physical items (`Copy`), availability, barcodes |
| `circulation` | Loans: desk checkout, self-service borrowing, check-in, renewals, lost items, `BorrowingPolicy` |
| `hold` | The holds queue: placing, readying a returned copy, expiry, cancellation |
| `fine` | The fines ledger: charges, payments and waivers (`FineTransaction`) |
| `member` | Users, roles, member types, card numbers, suspensions, staff accounts |
| `notification` | Member messages; `NotificationSender` (logs for now, swap in email/SMS) |
| `audit` | Append-only audit trail, written in the same transaction as the action |
| `jobs` | The daily job: expire holds, due-soon reminders, overdue notices |
| `report` | Circulation summaries and CSV exports |
| `settings` | Library-wide settings and the admin API |
| `selfservice` | `/api/me/**`: everything a member does for themselves |
| `dashboard`, `auth`, `seed`, `common` | Dashboards, login/registration, demo data, shared config, errors and security |

Design choices worth knowing:
- **Copies are first-class.** A loan points to a specific copy, so the desk scans real barcodes and history shows exactly which item went where. Availability is computed from copy statuses, never stored as a counter.
- **One place for each rule.** `BorrowingPolicy` gives the member's dashboard, the desk and the API the same answer, including every reason someone is blocked.
- **Money is a ledger.** Fines are never edited; each payment or waiver is a `FineTransaction` with the librarian who recorded it.
- **Schema migrations.** Flyway owns the schema (`db/migration`); Hibernate only validates that entities match it.
- **Audit in the same transaction.** `AuditService.record` requires an open transaction, so an action and its audit entry are saved together or not at all.
- **Concurrency.** Copies, titles and fines use optimistic locking (`@Version`), so two desks can't issue the same copy at once.
- **Defence in depth.** Every staff endpoint is annotated `@StaffOnly` or `@AdminOnly`, and the URL rules in `SecurityConfig` repeat the same restrictions.

### Roles
`MEMBER` borrows. `LIBRARIAN` runs the desk and the catalogue. `ADMIN` also sets rules, manages staff and reads the audit log.

### How login works (JWT)
1. The user posts their email and password to `/api/auth/login`.
2. Spring Security checks the password against the BCrypt hash.
3. The server returns a signed JWT (valid for 24 hours), sent back as `Authorization: Bearer <token>`.
4. `JwtAuthFilter` validates it on every request and loads the user.

---

## Getting started

### Prerequisites
- **Java 17+**
- **Maven 3.9+**, or open `backend` in IntelliJ IDEA (Maven is built in)
- **Node.js 18+**

### 1. Run the backend
```bash
cd backend
mvn spring-boot:run
```
The API starts at **http://localhost:8080**. On first run Flyway creates the schema and the seeder adds a demo library: 14 titles with copies on named shelves, loans (active, overdue, returned, lost), a holds queue, a hold ready for pickup, fines (paid, owed, part-waived), a damaged and a withdrawn copy.

The H2 database lives in `backend/data/shelfwise*`. Console: **http://localhost:8080/h2-console**, JDBC URL `jdbc:h2:file:./data/shelfwise`, user `sa`, empty password.

> Upgrading from the earlier version: the old `backend/data/librarydb*` files are no longer used and can be deleted once you no longer need them.

### 2. Run the frontend
```bash
cd frontend
npm install
npm run dev
```
Open **http://localhost:5173**. To point the dev server at a backend on another port: `API_TARGET=http://localhost:8081 npm run dev`.

### 3. Log in with a demo account
| Role | Email | Password |
|------|-------|----------|
| Admin | `admin@library.com` | `admin123` |
| Librarian | `librarian@library.com` | `librarian123` |
| Student | `student@library.com` | `student123` |
| Faculty | `faculty@library.com` | `faculty123` |
| Student, blocked (overdue book, unpaid fines) | `priya@library.com` | `priya123` |

Library card numbers are shown on each member's profile (e.g. `M000003`); copy barcodes look like `SW000013`. Try the desk: scan card `M000007` and issue `SW000013`, the copy waiting on the hold shelf for Ananya.

### Optional: MySQL instead of H2
```bash
docker compose up -d
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```
Uses a database called `shelfwise`. Credentials come from `DB_USERNAME` / `DB_PASSWORD`.

---

## Running tests
```bash
cd backend
mvn test
```
- `LibraryApiTest`: starts the whole app on an in-memory database (migrated by Flyway, filled by the seeder) and drives the real API: role restrictions, self-service borrowing, desk checkout and check-in, the holds queue handing a returned copy to the right member, renewal limits, blocked members, fine payments, CSV reports
- `FineTest`: the fine ledger rules without a database

---

## REST API

Self-service (any logged-in user, scoped to themselves)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/auth/register`, `/api/auth/login` | Create a member account / log in |
| GET | `/api/me` | The logged-in user |
| GET | `/api/me/dashboard` | Loans, holds, fines, eligibility, messages |
| GET / POST | `/api/me/loans` | My loans / borrow a title online `{bookId}` |
| POST | `/api/me/loans/{id}/renew` | Renew my loan |
| GET / POST | `/api/me/holds` | My holds / place a hold `{bookId}` |
| DELETE | `/api/me/holds/{id}` | Cancel my hold |
| GET | `/api/me/fines`, `/api/me/notifications` | My fines ledger / messages |
| GET | `/api/books`, `/api/books/{id}`, `/api/books/categories` | Search the catalogue |

Staff (librarian or admin)

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/api/desk/patrons/{card}` | Member standing for the desk |
| POST | `/api/desk/checkout` | Issue `{cardNumber, barcode}` |
| POST | `/api/desk/checkin` | Check in `{barcode}` |
| GET | `/api/loans?status=active\|overdue\|returned\|lost\|all` | Loans |
| POST | `/api/loans/{id}/checkin` · `/renew` · `/lost` | Loan actions |
| GET / DELETE | `/api/holds?status=ready\|waiting\|active`, `/api/holds/{id}` | Holds queue |
| GET | `/api/fines?status=open\|settled\|all` | Fines |
| POST | `/api/fines/{id}/payments` · `/waivers` | Pay `{amount, note}` / waive `{amount, reason}` |
| POST / PUT / DELETE | `/api/books`, `/api/books/{id}` | Manage titles |
| GET / POST | `/api/books/{id}/copies` | Copies of a title / add copies |
| GET | `/api/books/{id}/holds` | Queue for a title |
| PATCH | `/api/copies/{id}` | Status, shelf, note |
| GET | `/api/members`, `/api/members/{id}`, `/api/members/types` | Members |
| PUT / POST | `/api/members/{id}/type` · `/suspend` · `/reactivate` | Member actions |
| GET | `/api/dashboard/staff` | Staff dashboard |
| GET | `/api/reports/summary?from&to`, `/loans.csv`, `/fines.csv`, `/overdue.csv` | Reports |

Admin

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET / PUT | `/api/admin/settings` | Library settings and member types |
| PUT | `/api/admin/member-types/{id}` | Edit a member type's rules |
| GET / POST | `/api/admin/staff` | Staff accounts |
| GET | `/api/admin/audit?action&q&page` | Audit log |
| POST | `/api/admin/jobs/daily` | Run the daily job now |

---

## Possible enhancements
- Email or SMS delivery: provide a `NotificationSender` bean (JavaMail, Twilio)
- Online fine payment through a payment gateway
- Printable barcode labels and library cards
- Inter-branch transfers and multiple locations
- OpenAPI documentation (`springdoc-openapi`)

## Security notes
- Passwords are hashed with BCrypt; the API never returns them.
- The JWT secret in `application.properties` is for development. For a deployed app set `JWT_SECRET` (Base64, at least 32 bytes), e.g. `openssl rand -base64 48`.
- Staff endpoints are protected twice: method annotations and URL rules.
