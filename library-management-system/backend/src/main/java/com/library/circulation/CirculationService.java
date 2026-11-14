package com.library.circulation;

import com.library.audit.AuditAction;
import com.library.audit.AuditService;
import com.library.catalog.Copy;
import com.library.catalog.CopyRepository;
import com.library.catalog.CopyStatus;
import com.library.common.Dates;
import com.library.common.exception.BadRequestException;
import com.library.common.exception.ResourceNotFoundException;
import com.library.fine.FineService;
import com.library.hold.Hold;
import com.library.hold.HoldRepository;
import com.library.hold.HoldService;
import com.library.hold.HoldStatus;
import com.library.member.MemberType;
import com.library.member.User;
import com.library.member.UserRepository;
import com.library.settings.SettingsService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Loans from issue to check-in.
 *
 *   Issue        a librarian scans card + copy at the desk, or a member borrows online
 *                (the system picks a free copy, or the copy held for them)
 *   Check-in     librarians only; charges any late fine, then offers the copy to the holds queue
 *   Renew        members renew their own loans, staff renew any; blocked by holds or overdue
 *   Mark lost    librarians only; charges the lost item fee plus any late fine
 */
@Service
public class CirculationService {

    private final LoanRepository loanRepository;
    private final CopyRepository copyRepository;
    private final UserRepository userRepository;
    private final HoldRepository holdRepository;
    private final HoldService holdService;
    private final FineService fineService;
    private final BorrowingPolicy policy;
    private final SettingsService settingsService;
    private final AuditService auditService;
    private final Clock clock;

    public CirculationService(LoanRepository loanRepository, CopyRepository copyRepository,
                              UserRepository userRepository, HoldRepository holdRepository, HoldService holdService,
                              FineService fineService, BorrowingPolicy policy, SettingsService settingsService,
                              AuditService auditService, Clock clock) {
        this.loanRepository = loanRepository;
        this.copyRepository = copyRepository;
        this.userRepository = userRepository;
        this.holdRepository = holdRepository;
        this.holdService = holdService;
        this.fineService = fineService;
        this.policy = policy;
        this.settingsService = settingsService;
        this.auditService = auditService;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ issue

    @Transactional
    public LoanResponse checkoutAtDesk(CheckoutRequest request, User librarian) {
        User member = userRepository.findByCardNumber(request.cardNumber().trim().toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("No member with card " + request.cardNumber().trim()));
        Copy copy = findCopy(request.barcode());
        return issue(member, copy, LoanChannel.DESK, librarian);
    }

    @Transactional
    public LoanResponse selfBorrow(User member, Long bookId) {
        User fresh = reload(member);
        Copy copy = holdRepository.findFirstByMemberIdAndBookIdAndStatus(fresh.getId(), bookId, HoldStatus.READY)
                .map(Hold::getCopy)
                .or(() -> copyRepository.findFirstByBookIdAndStatusOrderByIdAsc(bookId, CopyStatus.AVAILABLE))
                .orElseThrow(() -> new BadRequestException("All copies are out. Place a hold to join the queue."));
        return issue(fresh, copy, LoanChannel.SELF_SERVICE, null);
    }

    private LoanResponse issue(User member, Copy copy, LoanChannel channel, User librarian) {
        LocalDate today = today();
        Long bookId = copy.getBook().getId();
        if (member.isStaff()) {
            throw new BadRequestException("Staff accounts can't borrow. Use a member card.");
        }

        Eligibility eligibility = policy.evaluate(member, today);
        if (!eligibility.canBorrow()) {
            throw new BadRequestException(subject(member, librarian) + " can't borrow: "
                    + String.join("; ", eligibility.blocks()));
        }
        if (loanRepository.existsByMemberIdAndCopyBookIdAndState(member.getId(), bookId, LoanState.OPEN)) {
            throw new BadRequestException(subject(member, librarian) + " already "
                    + (librarian == null ? "have" : "has") + " a copy of this title");
        }

        // Settle the member's hold on this title, if any, and check the copy is theirs to take.
        Optional<Hold> ownHold = holdRepository.findFirstByMemberIdAndBookIdAndStatus(member.getId(), bookId, HoldStatus.READY)
                .or(() -> holdRepository.findFirstByMemberIdAndBookIdAndStatus(member.getId(), bookId, HoldStatus.WAITING));
        switch (copy.getStatus()) {
            case AVAILABLE -> { }
            case ON_HOLD_SHELF -> {
                Hold heldFor = holdRepository.findFirstByCopyIdAndStatus(copy.getId(), HoldStatus.READY)
                        .orElseThrow(() -> new IllegalStateException("Copy on hold shelf without a READY hold"));
                if (!heldFor.getMember().getId().equals(member.getId())) {
                    throw new BadRequestException("Copy " + copy.getBarcode() + " is on the hold shelf for "
                            + heldFor.getMember().getName());
                }
            }
            default -> throw new BadRequestException("Copy " + copy.getBarcode() + " is "
                    + copy.getStatus().name().toLowerCase().replace('_', ' ') + " and can't be issued");
        }
        if (ownHold.isPresent()) {
            Hold hold = ownHold.get();
            Copy previouslyHeld = hold.getStatus() == HoldStatus.READY ? hold.getCopy() : null;
            holdService.fulfil(hold);
            // They took a different copy than the one set aside: pass the set-aside copy along.
            if (previouslyHeld != null && !previouslyHeld.getId().equals(copy.getId())) {
                holdService.allocate(previouslyHeld, librarian);
            }
        }

        MemberType type = member.getMemberType();
        copy.setStatus(CopyStatus.ON_LOAN);
        Loan loan = loanRepository.save(new Loan(copy, member, today, today.plusDays(type.getLoanDays()), channel, librarian));
        auditService.record(librarian == null ? member : librarian, AuditAction.CHECKOUT, "Loan", loan.getId(),
                "\"" + copy.getBook().getTitle() + "\" (" + copy.getBarcode() + ") issued to " + member.getName()
                        + (channel == LoanChannel.SELF_SERVICE ? " online" : " at the desk") + ", due " + Dates.human(loan.getDueDate()));
        return LoanResponse.from(loan, today);
    }

    // ------------------------------------------------------------------ check-in

    @Transactional
    public CheckInResult checkIn(String barcode, User librarian) {
        Copy copy = findCopy(barcode);
        Loan loan = loanRepository.findFirstByCopyIdAndState(copy.getId(), LoanState.OPEN)
                .orElseThrow(() -> new BadRequestException(notOnLoanMessage(copy)));
        return checkIn(loan, librarian);
    }

    @Transactional
    public CheckInResult checkInLoan(Long loanId, User librarian) {
        Loan loan = findLoan(loanId);
        if (!loan.isOpen()) {
            throw new BadRequestException("This loan is already closed");
        }
        return checkIn(loan, librarian);
    }

    private CheckInResult checkIn(Loan loan, User librarian) {
        LocalDate today = today();
        long daysLate = loan.daysOverdue(today);
        loan.checkIn(today, librarian);

        MemberType type = loan.getMember().getMemberType();
        int fine = fineService.assessLateReturn(loan, daysLate, type == null ? 0 : type.getFinePerDay(), librarian);
        Optional<Hold> readied = holdService.allocate(loan.getCopy(), librarian);

        auditService.record(librarian, AuditAction.CHECKIN, "Loan", loan.getId(),
                "\"" + loan.getCopy().getBook().getTitle() + "\" (" + loan.getCopy().getBarcode() + ") checked in from "
                        + loan.getMember().getName() + (daysLate > 0 ? ", " + daysLate + " days late" : ""));
        CheckInResult.HoldNotice notice = readied
                .map(h -> new CheckInResult.HoldNotice(h.getId(), h.getMember().getName(), h.getMember().getCardNumber(), h.getExpiresOn()))
                .orElse(null);
        return new CheckInResult(LoanResponse.from(loan, today), fine, notice, loan.getCopy().getShelfLocation());
    }

    // ------------------------------------------------------------------ renew & lost

    @Transactional
    public LoanResponse renew(Long loanId, User actor) {
        Loan loan = findLoan(loanId);
        if (!actor.isStaff() && !loan.getMember().getId().equals(actor.getId())) {
            throw new AccessDeniedException("This loan belongs to another member");
        }
        String blocked = renewBlockedReason(loan, today());
        if (blocked != null) {
            throw new BadRequestException(blocked);
        }
        LocalDate today = today();
        MemberType type = loan.getMember().getMemberType();
        LocalDate fromToday = today.plusDays(type.getLoanDays());
        LocalDate newDue = fromToday.isAfter(loan.getDueDate()) ? fromToday : loan.getDueDate().plusDays(type.getLoanDays());
        loan.renew(newDue);
        auditService.record(actor, AuditAction.RENEW, "Loan", loan.getId(),
                "\"" + loan.getCopy().getBook().getTitle() + "\" renewed for " + loan.getMember().getName()
                        + ", now due " + Dates.human(newDue) + " (renewal " + loan.getRenewalCount() + " of " + type.getMaxRenewals() + ")");
        return LoanResponse.from(loan, today);
    }

    @Transactional
    public LoanResponse markLost(Long loanId, User librarian) {
        Loan loan = findLoan(loanId);
        if (!loan.isOpen()) {
            throw new BadRequestException("Only a book that is still out can be marked lost");
        }
        LocalDate today = today();
        long daysLate = loan.daysOverdue(today);
        loan.markLost(today, librarian);
        loan.getCopy().setStatus(CopyStatus.LOST);

        MemberType type = loan.getMember().getMemberType();
        int late = fineService.assessLateReturn(loan, daysLate, type == null ? 0 : type.getFinePerDay(), librarian);
        int fee = fineService.assessLostItem(loan, settingsService.current().getLostItemFee(), librarian);
        auditService.record(librarian, AuditAction.MARK_LOST, "Loan", loan.getId(),
                "Copy " + loan.getCopy().getBarcode() + " of \"" + loan.getCopy().getBook().getTitle() + "\" lost by "
                        + loan.getMember().getName() + ", charged ₹" + (late + fee));
        return LoanResponse.from(loan, today);
    }

    // ------------------------------------------------------------------ queries

    /** A member's own loans, newest first, with whether each open loan can be renewed. */
    @Transactional(readOnly = true)
    public List<LoanResponse> loansForMember(Long memberId) {
        LocalDate today = today();
        return loanRepository.findByMemberIdOrderByBorrowDateDescIdDesc(memberId).stream()
                .map(l -> LoanResponse.from(l, today, l.isOpen() ? renewBlockedReason(l, today) : null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> openLoansForMember(Long memberId) {
        LocalDate today = today();
        return loanRepository.findByMemberIdAndStateOrderByDueDateAsc(memberId, LoanState.OPEN).stream()
                .map(l -> LoanResponse.from(l, today, renewBlockedReason(l, today)))
                .toList();
    }

    /** @param status all | active | overdue | returned | lost */
    @Transactional(readOnly = true)
    public List<LoanResponse> list(String status) {
        LocalDate today = today();
        List<Loan> loans = switch (status == null ? "all" : status.toLowerCase()) {
            case "active" -> loanRepository.findByStateOrderByDueDateAsc(LoanState.OPEN);
            case "overdue" -> loanRepository.findByStateAndDueDateBeforeOrderByDueDateAsc(LoanState.OPEN, today);
            case "returned" -> loanRepository.findByStateInOrderByReturnDateDesc(List.of(LoanState.RETURNED));
            case "lost" -> loanRepository.findByStateInOrderByReturnDateDesc(List.of(LoanState.LOST));
            case "all" -> loanRepository.findAllByOrderByBorrowDateDescIdDesc();
            default -> throw new BadRequestException("Unknown status: " + status);
        };
        return loans.stream().map(l -> LoanResponse.from(l, today)).toList();
    }

    @Transactional(readOnly = true)
    public Eligibility eligibility(User member) {
        return policy.evaluate(member, today());
    }

    /** Why this open loan can't be renewed right now, or null if it can. */
    String renewBlockedReason(Loan loan, LocalDate today) {
        if (!loan.isOpen()) {
            return "This loan is closed";
        }
        MemberType type = loan.getMember().getMemberType();
        if (type == null) {
            return "No member type set";
        }
        if (loan.getRenewalCount() >= type.getMaxRenewals()) {
            return type.getMaxRenewals() == 0 ? "Renewals aren't allowed for " + type.getName().toLowerCase() + " members"
                    : "No renewals left (" + loan.getRenewalCount() + " of " + type.getMaxRenewals() + " used)";
        }
        if (loan.isOverdue(today)) {
            return "Overdue books can't be renewed. Return it at the desk.";
        }
        if (holdRepository.countByBookIdAndStatus(loan.getCopy().getBook().getId(), HoldStatus.WAITING) > 0) {
            return "Another member is waiting for this title";
        }
        List<String> blocks = policy.renewalBlocks(loan.getMember());
        return blocks.isEmpty() ? null : String.join("; ", blocks);
    }

    // ------------------------------------------------------------------ helpers

    private String notOnLoanMessage(Copy copy) {
        if (copy.getStatus() == CopyStatus.ON_HOLD_SHELF) {
            return "Copy " + copy.getBarcode() + " is already checked in and waiting on the hold shelf";
        }
        return "Copy " + copy.getBarcode() + " is not on loan (it's " + copy.getStatus().name().toLowerCase().replace('_', ' ') + ")";
    }

    private static String subject(User member, User librarian) {
        return librarian == null ? "You" : member.getName();
    }

    private User reload(User member) {
        return userRepository.findByEmail(member.getEmail())
                .orElseThrow(() -> ResourceNotFoundException.of("Member", member.getId()));
    }

    private Copy findCopy(String barcode) {
        String code = barcode.trim().toUpperCase();
        return copyRepository.findByBarcode(code)
                .orElseThrow(() -> new ResourceNotFoundException("No copy with barcode " + code));
    }

    private Loan findLoan(Long id) {
        return loanRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Loan", id));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }
}
