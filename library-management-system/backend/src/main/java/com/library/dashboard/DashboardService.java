package com.library.dashboard;

import com.library.audit.AuditQueryService;
import com.library.catalog.BookRepository;
import com.library.catalog.CopyRepository;
import com.library.catalog.CopyStatus;
import com.library.circulation.CirculationService;
import com.library.circulation.LoanRepository;
import com.library.circulation.LoanState;
import com.library.fine.FineRepository;
import com.library.fine.FineTransactionRepository;
import com.library.hold.HoldRepository;
import com.library.hold.HoldService;
import com.library.hold.HoldStatus;
import com.library.member.Role;
import com.library.member.User;
import com.library.member.UserRepository;
import com.library.member.UserResponse;
import com.library.notification.NotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class DashboardService {

    private final BookRepository bookRepository;
    private final CopyRepository copyRepository;
    private final LoanRepository loanRepository;
    private final HoldRepository holdRepository;
    private final FineRepository fineRepository;
    private final FineTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final CirculationService circulationService;
    private final HoldService holdService;
    private final NotificationService notificationService;
    private final AuditQueryService auditQueryService;
    private final Clock clock;

    public DashboardService(BookRepository bookRepository, CopyRepository copyRepository, LoanRepository loanRepository,
                            HoldRepository holdRepository, FineRepository fineRepository,
                            FineTransactionRepository transactionRepository, UserRepository userRepository,
                            CirculationService circulationService, HoldService holdService,
                            NotificationService notificationService, AuditQueryService auditQueryService, Clock clock) {
        this.bookRepository = bookRepository;
        this.copyRepository = copyRepository;
        this.loanRepository = loanRepository;
        this.holdRepository = holdRepository;
        this.fineRepository = fineRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.circulationService = circulationService;
        this.holdService = holdService;
        this.notificationService = notificationService;
        this.auditQueryService = auditQueryService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public StaffDashboard staff() {
        LocalDate today = LocalDate.now(clock);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (CopyStatus s : CopyStatus.values()) {
            byStatus.put(s.name(), 0L);
        }
        for (Object[] row : copyRepository.countAllByStatus()) {
            byStatus.put(((CopyStatus) row[0]).name(), (Long) row[1]);
        }
        // An overdue copy is still ON_LOAN; split it out so the stack map can show it.
        long overdue = loanRepository.countByStateAndDueDateBefore(LoanState.OPEN, today);
        byStatus.put("OVERDUE", overdue);
        byStatus.put(CopyStatus.ON_LOAN.name(), Math.max(byStatus.get(CopyStatus.ON_LOAN.name()) - overdue, 0));

        return new StaffDashboard(
                bookRepository.count(),
                userRepository.countByRole(Role.MEMBER),
                loanRepository.countByState(LoanState.OPEN),
                overdue,
                holdRepository.countByStatus(HoldStatus.WAITING),
                holdRepository.countByStatus(HoldStatus.READY),
                fineRepository.outstandingTotal(),
                transactionRepository.totalCollected(),
                byStatus,
                auditQueryService.recent(8));
    }

    @Transactional(readOnly = true)
    public MemberDashboard member(User user) {
        User member = userRepository.findByEmail(user.getEmail()).orElse(user);
        return new MemberDashboard(
                UserResponse.from(member),
                circulationService.eligibility(member),
                circulationService.openLoansForMember(member.getId()),
                holdService.activeForMember(member.getId()),
                loanRepository.countByMemberId(member.getId()),
                fineRepository.outstandingForMember(member.getId()),
                fineRepository.paidByMember(member.getId()),
                notificationService.recentFor(member.getId()));
    }
}
