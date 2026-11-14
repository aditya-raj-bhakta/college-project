package com.library.seed;

import com.library.catalog.BookRequest;
import com.library.catalog.BookResponse;
import com.library.catalog.CatalogService;
import com.library.catalog.Copy;
import com.library.catalog.CopyRepository;
import com.library.catalog.CopyStatus;
import com.library.catalog.CopyUpdateRequest;
import com.library.circulation.CirculationService;
import com.library.circulation.Loan;
import com.library.circulation.LoanChannel;
import com.library.circulation.LoanRepository;
import com.library.circulation.LoanState;
import com.library.fine.Fine;
import com.library.fine.FineRepository;
import com.library.fine.FineService;
import com.library.fine.FineType;
import com.library.fine.PaymentRequest;
import com.library.fine.WaiverRequest;
import com.library.hold.HoldService;
import com.library.jobs.DailyJobs;
import com.library.member.CardNumbers;
import com.library.member.MemberType;
import com.library.member.MemberTypeRepository;
import com.library.member.Role;
import com.library.member.User;
import com.library.member.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fills an empty database with a small, believable library so every screen has something to show.
 * History (past loans) is written directly; everything else goes through the real services so the
 * holds queue, fines ledger, notifications and audit log are consistent.
 *
 *   Admin      admin@library.com      / admin123
 *   Librarian  librarian@library.com  / librarian123
 *   Members    student@library.com    / student123   (student)
 *              faculty@library.com    / faculty123   (faculty)
 *              priya@library.com      / priya123     (student, blocked: overdue book and unpaid fines)
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final MemberTypeRepository memberTypeRepository;
    private final CopyRepository copyRepository;
    private final LoanRepository loanRepository;
    private final FineRepository fineRepository;
    private final CatalogService catalogService;
    private final CirculationService circulationService;
    private final HoldService holdService;
    private final FineService fineService;
    private final DailyJobs dailyJobs;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DataSeeder(UserRepository userRepository, MemberTypeRepository memberTypeRepository,
                      CopyRepository copyRepository, LoanRepository loanRepository, FineRepository fineRepository,
                      CatalogService catalogService, CirculationService circulationService, HoldService holdService,
                      FineService fineService, DailyJobs dailyJobs, PasswordEncoder passwordEncoder, Clock clock) {
        this.userRepository = userRepository;
        this.memberTypeRepository = memberTypeRepository;
        this.copyRepository = copyRepository;
        this.loanRepository = loanRepository;
        this.fineRepository = fineRepository;
        this.catalogService = catalogService;
        this.circulationService = circulationService;
        this.holdService = holdService;
        this.fineService = fineService;
        this.dailyJobs = dailyJobs;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        log.info("Empty database - adding demo library");
        LocalDate t = LocalDate.now(clock);
        Map<String, MemberType> types = new HashMap<>();
        memberTypeRepository.findAll().forEach(mt -> types.put(mt.getCode(), mt));

        // ----- people -----
        User admin = staff("Library Admin", "admin@library.com", "admin123", Role.ADMIN);
        User librarian = staff("Meera Nair", "librarian@library.com", "librarian123", Role.LIBRARIAN);
        User rahul = member("Rahul Sharma", "student@library.com", "student123", types.get("STUDENT"));
        User anita = member("Dr. Anita Rao", "faculty@library.com", "faculty123", types.get("FACULTY"));
        User priya = member("Priya Verma", "priya@library.com", "priya123", types.get("STUDENT"));
        User kabir = member("Kabir Singh", "kabir@library.com", "kabir123", types.get("STAFF"));
        User ananya = member("Ananya Iyer", "ananya@library.com", "ananya123", types.get("STUDENT"));

        // ----- catalogue -----
        Map<String, List<Copy>> copies = new HashMap<>();
        book(copies, admin, "Clean Code", "Robert C. Martin", "978-0132350884", "Programming", 2008, 4, "Bay A2",
                "A handbook of agile software craftsmanship: how to write code that is easy to read and change.");
        book(copies, admin, "Effective Java", "Joshua Bloch", "978-0134685991", "Programming", 2018, 3, "Bay A2",
                "Best practices for the Java platform, covering generics, lambdas, streams and more.");
        book(copies, admin, "Head First Java", "Kathy Sierra, Bert Bates", "978-1491910771", "Programming", 2022, 5, "Bay A3",
                "A beginner-friendly, visual introduction to Java and object-oriented programming.");
        book(copies, admin, "Spring in Action", "Craig Walls", "978-1617297571", "Programming", 2022, 2, "Bay A3",
                "Building web applications and microservices with the Spring Framework and Spring Boot.");
        book(copies, admin, "Introduction to Algorithms", "Thomas H. Cormen et al.", "978-0262046305", "Computer Science", 2022, 3, "Bay B1",
                "The classic reference on algorithms and data structures (CLRS).");
        book(copies, admin, "Operating System Concepts", "Abraham Silberschatz", "978-1119800361", "Computer Science", 2021, 3, "Bay B1",
                "Processes, memory, file systems and concurrency - the 'dinosaur book'.");
        book(copies, admin, "Database System Concepts", "Abraham Silberschatz", "978-0078022159", "Computer Science", 2019, 2, "Bay B2",
                "Relational model, SQL, normalisation, transactions and indexing.");
        book(copies, admin, "Computer Networking: A Top-Down Approach", "James Kurose, Keith Ross", "978-0136681557", "Computer Science", 2021, 2, "Bay B2",
                "Networking explained from the application layer down to the physical layer.");
        book(copies, admin, "The Pragmatic Programmer", "David Thomas, Andrew Hunt", "978-0135957059", "Software Engineering", 2019, 3, "Bay C1",
                "Practical advice for becoming a better, more effective developer.");
        book(copies, admin, "Design Patterns", "Erich Gamma et al.", "978-0201633610", "Software Engineering", 1994, 2, "Bay C1",
                "The Gang of Four catalogue of reusable object-oriented design patterns.");
        book(copies, admin, "Wings of Fire", "A. P. J. Abdul Kalam", "978-8173711466", "Biography", 1999, 4, "Bay D1",
                "The autobiography of India's 'Missile Man' and former President.");
        book(copies, admin, "Atomic Habits", "James Clear", "978-0735211292", "Self Help", 2018, 3, "Bay D2",
                "An easy and proven way to build good habits and break bad ones.");
        book(copies, admin, "The Alchemist", "Paulo Coelho", "978-0062315007", "Fiction", 1988, 3, "Bay E1",
                "A shepherd boy's journey to find treasure and his personal legend.");
        book(copies, admin, "Sapiens", "Yuval Noah Harari", "978-0062316097", "History", 2015, 2, "Bay F1",
                "A brief history of humankind, from the Stone Age to the present.");

        // ----- loans already out (written directly, with past dates) -----
        open(copies.get("Effective Java").get(0), rahul, t.minusDays(5), t.plusDays(9), LoanChannel.SELF_SERVICE, null);
        open(copies.get("Clean Code").get(0), priya, t.minusDays(20), t.minusDays(6), LoanChannel.DESK, librarian);
        open(copies.get("Atomic Habits").get(0), priya, t.minusDays(2), t.plusDays(12), LoanChannel.SELF_SERVICE, null);
        open(copies.get("Design Patterns").get(0), anita, t.minusDays(40), t.plusDays(20), LoanChannel.DESK, librarian);
        open(copies.get("Database System Concepts").get(0), anita, t.minusDays(10), t.plusDays(50), LoanChannel.SELF_SERVICE, null);
        open(copies.get("Sapiens").get(0), kabir, t.minusDays(8), t.plusDays(22), LoanChannel.DESK, librarian);
        open(copies.get("Sapiens").get(1), ananya, t.minusDays(3), t.plusDays(11), LoanChannel.SELF_SERVICE, null);
        Loan springA = open(copies.get("Spring in Action").get(0), kabir, t.minusDays(16), t.plusDays(14), LoanChannel.DESK, librarian);
        open(copies.get("Spring in Action").get(1), anita, t.minusDays(12), t.plusDays(48), LoanChannel.DESK, librarian);

        // ----- history: returned late (one fine paid, one still owed) and a lost copy -----
        Loan clrs = closed(copies.get("Introduction to Algorithms").get(0), rahul, t.minusDays(30), t.minusDays(16), t.minusDays(12), LoanState.RETURNED, librarian);
        Fine rahulFine = fineRepository.save(new Fine(rahul, clrs, FineType.LATE_RETURN, 40, "4 days late: Introduction to Algorithms", t.minusDays(12)));
        fineService.pay(rahulFine.getId(), new PaymentRequest(40, "Cash at desk"), librarian);

        Loan prag = closed(copies.get("The Pragmatic Programmer").get(0), kabir, t.minusDays(40), t.minusDays(10), t.minusDays(1), LoanState.RETURNED, librarian);
        fineRepository.save(new Fine(kabir, prag, FineType.LATE_RETURN, 45, "9 days late: The Pragmatic Programmer", t.minusDays(1)));

        Copy lostCopy = copies.get("The Alchemist").get(0);
        Loan lost = closed(lostCopy, priya, t.minusDays(45), t.minusDays(31), t.minusDays(25), LoanState.LOST, librarian);
        lostCopy.setStatus(CopyStatus.LOST);
        Fine lostFine = fineRepository.save(new Fine(priya, lost, FineType.LOST_ITEM, 500, "Lost copy " + lostCopy.getBarcode() + ": The Alchemist", t.minusDays(25)));
        fineService.waive(lostFine.getId(), new WaiverRequest(200, "Brought a replacement paperback"), librarian);

        // ----- copies out of circulation -----
        Copy damaged = copies.get("Head First Java").get(4);
        catalogService.updateCopy(damaged.getId(), new CopyUpdateRequest(CopyStatus.DAMAGED, damaged.getShelfLocation(), "Water damage, spine split"), librarian);
        Copy withdrawn = copies.get("Wings of Fire").get(3);
        catalogService.updateCopy(withdrawn.getId(), new CopyUpdateRequest(CopyStatus.WITHDRAWN, withdrawn.getShelfLocation(), "Superseded by new edition"), librarian);

        // ----- holds queue -----
        holdService.place(rahul, idOf(copies, "Sapiens"));          // both copies out: Rahul is first in line
        holdService.place(anita, idOf(copies, "Sapiens"));          // Anita second
        holdService.place(ananya, idOf(copies, "Spring in Action")); // then Kabir brings his copy back...
        circulationService.checkIn(springA.getCopy().getBarcode(), librarian); // ...and it goes to the hold shelf for Ananya

        // ----- reminders for anything due soon or overdue -----
        dailyJobs.run(null);

        log.info("Demo library ready. Logins: admin@library.com / admin123, librarian@library.com / librarian123, "
                + "student@library.com / student123, faculty@library.com / faculty123");
    }

    private User staff(String name, String email, String password, Role role) {
        return userRepository.save(new User(name, email, passwordEncoder.encode(password), role));
    }

    private User member(String name, String email, String password, MemberType type) {
        User u = new User(name, email, passwordEncoder.encode(password), Role.MEMBER);
        u.setMemberType(type);
        userRepository.save(u);
        u.setCardNumber(CardNumbers.forMember(u.getId()));
        return u;
    }

    private void book(Map<String, List<Copy>> copies, User admin, String title, String author, String isbn, String category,
                      int year, int count, String shelf, String description) {
        BookResponse b = catalogService.create(new BookRequest(title, author, isbn, category, year, description, count, shelf), admin);
        copies.put(title, copyRepository.findByBookIdOrderByIdAsc(b.id()));
    }

    private Long idOf(Map<String, List<Copy>> copies, String title) {
        return copies.get(title).get(0).getBook().getId();
    }

    private Loan open(Copy copy, User member, LocalDate borrowed, LocalDate due, LoanChannel channel, User issuedBy) {
        copy.setStatus(CopyStatus.ON_LOAN);
        return loanRepository.save(new Loan(copy, member, borrowed, due, channel, issuedBy));
    }

    private Loan closed(Copy copy, User member, LocalDate borrowed, LocalDate due, LocalDate returned, LoanState state, User librarian) {
        Loan loan = new Loan(copy, member, borrowed, due, LoanChannel.DESK, librarian);
        loan.setState(state);
        loan.setReturnDate(returned);
        return loanRepository.save(loan);
    }
}
