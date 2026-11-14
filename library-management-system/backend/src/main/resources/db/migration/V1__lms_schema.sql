-- Shelfwise LMS schema.
-- Written in SQL that runs on both H2 and MySQL.

-- ---------------------------------------------------------------
-- Rules
-- ---------------------------------------------------------------

-- Each member belongs to a type with its own borrowing limits.
CREATE TABLE member_types (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    code                  VARCHAR(20)  NOT NULL UNIQUE,
    name                  VARCHAR(60)  NOT NULL,
    max_loans             INT          NOT NULL,
    loan_days             INT          NOT NULL,
    max_renewals          INT          NOT NULL,
    fine_per_day          INT          NOT NULL,
    fine_block_threshold  INT          NOT NULL,
    max_holds             INT          NOT NULL
);

-- Library-wide settings. Always exactly one row, id = 1.
CREATE TABLE library_settings (
    id                    BIGINT       PRIMARY KEY,
    hold_pickup_days      INT          NOT NULL,
    lost_item_fee         INT          NOT NULL,
    reminder_days_before  INT          NOT NULL,
    default_member_type_id BIGINT      NOT NULL,
    CONSTRAINT fk_settings_member_type FOREIGN KEY (default_member_type_id) REFERENCES member_types (id)
);

-- ---------------------------------------------------------------
-- People
-- ---------------------------------------------------------------

CREATE TABLE users (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(100) NOT NULL,
    email             VARCHAR(150) NOT NULL UNIQUE,
    password          VARCHAR(255) NOT NULL,
    role              VARCHAR(20)  NOT NULL,
    card_number       VARCHAR(20)  UNIQUE,
    member_type_id    BIGINT,
    status            VARCHAR(20)  NOT NULL,
    suspended_reason  VARCHAR(255),
    created_at        TIMESTAMP    NOT NULL,
    CONSTRAINT fk_users_member_type FOREIGN KEY (member_type_id) REFERENCES member_types (id)
);

-- ---------------------------------------------------------------
-- Catalogue: a book is the title, a copy is the physical item on the shelf
-- ---------------------------------------------------------------

CREATE TABLE books (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    title           VARCHAR(200)  NOT NULL,
    author          VARCHAR(150)  NOT NULL,
    isbn            VARCHAR(20)   NOT NULL UNIQUE,
    category        VARCHAR(60),
    published_year  INT,
    description     VARCHAR(1000),
    created_at      TIMESTAMP     NOT NULL,
    version         BIGINT
);

CREATE TABLE copies (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id         BIGINT        NOT NULL,
    barcode         VARCHAR(40)   NOT NULL UNIQUE,
    shelf_location  VARCHAR(60),
    status          VARCHAR(20)   NOT NULL,
    note            VARCHAR(255),
    acquired_on     DATE          NOT NULL,
    version         BIGINT,
    CONSTRAINT fk_copies_book FOREIGN KEY (book_id) REFERENCES books (id)
);

CREATE INDEX idx_copies_book_status ON copies (book_id, status);

-- ---------------------------------------------------------------
-- Circulation
-- ---------------------------------------------------------------

CREATE TABLE loans (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    copy_id         BIGINT        NOT NULL,
    member_id       BIGINT        NOT NULL,
    borrow_date     DATE          NOT NULL,
    due_date        DATE          NOT NULL,
    return_date     DATE,
    state           VARCHAR(20)   NOT NULL,
    channel         VARCHAR(20)   NOT NULL,
    renewal_count   INT           NOT NULL,
    issued_by_id    BIGINT,
    received_by_id  BIGINT,
    CONSTRAINT fk_loans_copy        FOREIGN KEY (copy_id)        REFERENCES copies (id),
    CONSTRAINT fk_loans_member      FOREIGN KEY (member_id)      REFERENCES users (id),
    CONSTRAINT fk_loans_issued_by   FOREIGN KEY (issued_by_id)   REFERENCES users (id),
    CONSTRAINT fk_loans_received_by FOREIGN KEY (received_by_id) REFERENCES users (id)
);

CREATE INDEX idx_loans_member_state ON loans (member_id, state);
CREATE INDEX idx_loans_state_due    ON loans (state, due_date);
CREATE INDEX idx_loans_copy_state   ON loans (copy_id, state);

-- A member's place in the queue for a title. When a copy comes back it is held for
-- the first WAITING hold (status READY) until expires_on.
CREATE TABLE holds (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id     BIGINT       NOT NULL,
    member_id   BIGINT       NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    placed_at   TIMESTAMP    NOT NULL,
    copy_id     BIGINT,
    ready_on    DATE,
    expires_on  DATE,
    closed_at   TIMESTAMP,
    CONSTRAINT fk_holds_book   FOREIGN KEY (book_id)   REFERENCES books (id),
    CONSTRAINT fk_holds_member FOREIGN KEY (member_id) REFERENCES users (id),
    CONSTRAINT fk_holds_copy   FOREIGN KEY (copy_id)   REFERENCES copies (id)
);

CREATE INDEX idx_holds_book_status   ON holds (book_id, status, placed_at);
CREATE INDEX idx_holds_member_status ON holds (member_id, status);

-- ---------------------------------------------------------------
-- Fines: a charge, and every payment or waiver against it
-- ---------------------------------------------------------------

CREATE TABLE fines (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id    BIGINT        NOT NULL,
    loan_id      BIGINT,
    type         VARCHAR(20)   NOT NULL,
    amount       INT           NOT NULL,
    paid         INT           NOT NULL,
    waived       INT           NOT NULL,
    description  VARCHAR(255)  NOT NULL,
    created_on   DATE          NOT NULL,
    version      BIGINT,
    CONSTRAINT fk_fines_member FOREIGN KEY (member_id) REFERENCES users (id),
    CONSTRAINT fk_fines_loan   FOREIGN KEY (loan_id)   REFERENCES loans (id)
);

CREATE INDEX idx_fines_member ON fines (member_id);

CREATE TABLE fine_transactions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    fine_id     BIGINT        NOT NULL,
    kind        VARCHAR(20)   NOT NULL,
    amount      INT           NOT NULL,
    note        VARCHAR(255),
    staff_id    BIGINT        NOT NULL,
    created_at  TIMESTAMP     NOT NULL,
    CONSTRAINT fk_fine_tx_fine  FOREIGN KEY (fine_id)  REFERENCES fines (id),
    CONSTRAINT fk_fine_tx_staff FOREIGN KEY (staff_id) REFERENCES users (id)
);

-- ---------------------------------------------------------------
-- Notifications and audit trail
-- ---------------------------------------------------------------

CREATE TABLE notifications (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id  BIGINT        NOT NULL,
    kind       VARCHAR(20)   NOT NULL,
    loan_id    BIGINT,
    hold_id    BIGINT,
    message    VARCHAR(500)  NOT NULL,
    sent_at    TIMESTAMP     NOT NULL,
    CONSTRAINT fk_notifications_member FOREIGN KEY (member_id) REFERENCES users (id),
    CONSTRAINT fk_notifications_loan   FOREIGN KEY (loan_id)   REFERENCES loans (id),
    CONSTRAINT fk_notifications_hold   FOREIGN KEY (hold_id)   REFERENCES holds (id)
);

CREATE INDEX idx_notifications_member ON notifications (member_id, sent_at);

CREATE TABLE audit_events (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    occurred_at  TIMESTAMP     NOT NULL,
    actor_id     BIGINT,
    actor_name   VARCHAR(100)  NOT NULL,
    action       VARCHAR(40)   NOT NULL,
    target_type  VARCHAR(30)   NOT NULL,
    target_id    BIGINT,
    summary      VARCHAR(500)  NOT NULL
);

CREATE INDEX idx_audit_occurred ON audit_events (occurred_at);

-- ---------------------------------------------------------------
-- Default rules. Admins edit these on the Rules page.
-- ---------------------------------------------------------------

INSERT INTO member_types (code, name, max_loans, loan_days, max_renewals, fine_per_day, fine_block_threshold, max_holds)
VALUES ('STUDENT', 'Student', 3, 14, 1, 10, 100, 3),
       ('FACULTY', 'Faculty', 10, 60, 3, 5, 500, 10),
       ('STAFF',   'Staff',   5, 30, 2, 5, 200, 5);

INSERT INTO library_settings (id, hold_pickup_days, lost_item_fee, reminder_days_before, default_member_type_id)
SELECT 1, 3, 500, 2, id FROM member_types WHERE code = 'STUDENT';
