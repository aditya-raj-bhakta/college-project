package com.library.member;

import com.library.audit.AuditAction;
import com.library.audit.AuditService;
import com.library.circulation.CirculationService;
import com.library.circulation.LoanRepository;
import com.library.circulation.LoanState;
import com.library.common.exception.BadRequestException;
import com.library.common.exception.ResourceNotFoundException;
import com.library.common.web.PageResponse;
import com.library.fine.FineRepository;
import com.library.fine.FineService;
import com.library.hold.HoldService;
import com.library.notification.NotificationService;
import com.library.settings.SettingsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Member accounts as librarians see them, plus staff accounts for admins. */
@Service
public class MemberService {

    private final UserRepository userRepository;
    private final LoanRepository loanRepository;
    private final FineRepository fineRepository;
    private final CirculationService circulationService;
    private final HoldService holdService;
    private final FineService fineService;
    private final NotificationService notificationService;
    private final SettingsService settingsService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final Clock clock;

    public MemberService(UserRepository userRepository, LoanRepository loanRepository, FineRepository fineRepository,
                         CirculationService circulationService, HoldService holdService, FineService fineService,
                         NotificationService notificationService, SettingsService settingsService,
                         PasswordEncoder passwordEncoder, AuditService auditService, Clock clock) {
        this.userRepository = userRepository;
        this.loanRepository = loanRepository;
        this.fineRepository = fineRepository;
        this.circulationService = circulationService;
        this.holdService = holdService;
        this.fineService = fineService;
        this.notificationService = notificationService;
        this.settingsService = settingsService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ members

    @Transactional(readOnly = true)
    public PageResponse<MemberSummary> search(String q, AccountStatus status, Long typeId, int page, int size) {
        Page<User> members = userRepository.searchMembers(blankToNull(q), status, typeId,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("name")));
        LocalDate today = LocalDate.now(clock);
        return PageResponse.from(members, u -> new MemberSummary(u.getId(), u.getName(), u.getEmail(), u.getCardNumber(),
                u.getMemberType() == null ? null : u.getMemberType().getName(), u.getStatus(),
                loanRepository.countByMemberIdAndState(u.getId(), LoanState.OPEN),
                loanRepository.countByMemberIdAndStateAndDueDateBefore(u.getId(), LoanState.OPEN, today),
                fineRepository.outstandingForMember(u.getId()),
                loanRepository.countByMemberId(u.getId()),
                u.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public MemberDetail detail(Long id) {
        return detail(findMember(id));
    }

    /** What the desk shows when a library card is scanned. */
    @Transactional(readOnly = true)
    public MemberDetail detailByCard(String cardNumber) {
        String card = cardNumber.trim().toUpperCase();
        User member = userRepository.findByCardNumber(card)
                .orElseThrow(() -> new ResourceNotFoundException("No member with card " + card));
        return detail(member);
    }

    private MemberDetail detail(User member) {
        return new MemberDetail(UserResponse.from(member),
                circulationService.eligibility(member),
                circulationService.loansForMember(member.getId()),
                holdService.forMember(member.getId()),
                fineService.forMember(member.getId()),
                notificationService.recentFor(member.getId()));
    }

    @Transactional
    public MemberDetail changeType(Long id, ChangeTypeRequest request, User staff) {
        User member = findMember(id);
        MemberType type = settingsService.findType(request.memberTypeId());
        member.setMemberType(type);
        auditService.record(staff, AuditAction.MEMBER_TYPE_CHANGED, "User", member.getId(),
                member.getName() + " is now a " + type.getName().toLowerCase() + " member");
        return detail(member);
    }

    @Transactional
    public MemberDetail suspend(Long id, SuspendRequest request, User staff) {
        User member = findMember(id);
        if (member.isSuspended()) {
            throw new BadRequestException(member.getName() + " is already suspended");
        }
        member.suspend(request.reason().trim());
        auditService.record(staff, AuditAction.MEMBER_SUSPENDED, "User", member.getId(),
                member.getName() + " suspended: " + request.reason().trim());
        return detail(member);
    }

    @Transactional
    public MemberDetail reactivate(Long id, User staff) {
        User member = findMember(id);
        if (!member.isSuspended()) {
            throw new BadRequestException(member.getName() + " is not suspended");
        }
        member.reactivate();
        auditService.record(staff, AuditAction.MEMBER_REACTIVATED, "User", member.getId(), member.getName() + " reactivated");
        return detail(member);
    }

    // ------------------------------------------------------------------ staff (admin)

    @Transactional(readOnly = true)
    public List<UserResponse> staff() {
        return userRepository.findByRoleInOrderByNameAsc(List.of(Role.LIBRARIAN, Role.ADMIN)).stream()
                .map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse createStaff(StaffRequest request, User admin) {
        if (!request.role().isStaff()) {
            throw new BadRequestException("Staff accounts are librarians or admins");
        }
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("An account with this email already exists");
        }
        User user = userRepository.save(new User(request.name().trim(), email, passwordEncoder.encode(request.password()), request.role()));
        auditService.record(admin, AuditAction.STAFF_CREATED, "User", user.getId(),
                "Created " + request.role().name().toLowerCase() + " account for " + user.getName());
        return UserResponse.from(user);
    }

    private User findMember(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Member", id));
        if (user.isStaff()) {
            throw new ResourceNotFoundException("Member not found: " + id);
        }
        return user;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
