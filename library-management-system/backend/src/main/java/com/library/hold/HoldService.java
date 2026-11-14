package com.library.hold;

import com.library.audit.AuditAction;
import com.library.audit.AuditService;
import com.library.catalog.Book;
import com.library.catalog.BookRepository;
import com.library.catalog.Copy;
import com.library.catalog.CopyRepository;
import com.library.catalog.CopyStatus;
import com.library.circulation.LoanRepository;
import com.library.circulation.LoanState;
import com.library.common.Dates;
import com.library.common.exception.BadRequestException;
import com.library.common.exception.ResourceNotFoundException;
import com.library.fine.FineRepository;
import com.library.member.MemberType;
import com.library.member.User;
import com.library.notification.NotificationService;
import com.library.settings.SettingsService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The holds queue. Members join the queue for a title whose copies are all out.
 * Whenever a copy becomes free (checked in, repaired, newly added, or released by an
 * expired or cancelled hold) {@link #allocate} offers it to the first member in line.
 */
@Service
public class HoldService {

    private static final Set<HoldStatus> ACTIVE = EnumSet.of(HoldStatus.WAITING, HoldStatus.READY);

    private final HoldRepository holdRepository;
    private final BookRepository bookRepository;
    private final CopyRepository copyRepository;
    private final LoanRepository loanRepository;
    private final FineRepository fineRepository;
    private final NotificationService notificationService;
    private final SettingsService settingsService;
    private final AuditService auditService;
    private final Clock clock;

    public HoldService(HoldRepository holdRepository, BookRepository bookRepository, CopyRepository copyRepository,
                       LoanRepository loanRepository, FineRepository fineRepository,
                       NotificationService notificationService, SettingsService settingsService,
                       AuditService auditService, Clock clock) {
        this.holdRepository = holdRepository;
        this.bookRepository = bookRepository;
        this.copyRepository = copyRepository;
        this.loanRepository = loanRepository;
        this.fineRepository = fineRepository;
        this.notificationService = notificationService;
        this.settingsService = settingsService;
        this.auditService = auditService;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ commands

    @Transactional
    public HoldResponse place(User member, Long bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> ResourceNotFoundException.of("Book", bookId));
        MemberType type = member.getMemberType();

        if (member.isSuspended()) {
            throw new BadRequestException("Your account is suspended, so you can't place holds");
        }
        if (type == null) {
            throw new BadRequestException("Your account has no member type yet. Ask at the desk.");
        }
        long owed = fineRepository.outstandingForMember(member.getId());
        if (owed > type.getFineBlockThreshold()) {
            throw new BadRequestException("Unpaid fines of ₹" + owed + " are over the ₹" + type.getFineBlockThreshold()
                    + " limit. Pay at the desk to place holds.");
        }
        if (holdRepository.existsByMemberIdAndBookIdAndStatusIn(member.getId(), bookId, ACTIVE)) {
            throw new BadRequestException("You already have a hold on this title");
        }
        if (loanRepository.existsByMemberIdAndCopyBookIdAndState(member.getId(), bookId, LoanState.OPEN)) {
            throw new BadRequestException("You already have this title borrowed");
        }
        if (holdRepository.countByMemberIdAndStatusIn(member.getId(), ACTIVE) >= type.getMaxHolds()) {
            throw new BadRequestException("You can have at most " + type.getMaxHolds() + " holds at a time");
        }
        if (!copyRepository.existsByBookIdAndStatusIn(bookId, EnumSet.of(CopyStatus.ON_LOAN, CopyStatus.ON_HOLD_SHELF, CopyStatus.AVAILABLE))) {
            throw new BadRequestException("No copies of this title are in circulation");
        }
        boolean freeCopy = copyRepository.findFirstByBookIdAndStatusOrderByIdAsc(bookId, CopyStatus.AVAILABLE).isPresent();
        if (freeCopy) {
            throw new BadRequestException("A copy is on the shelf right now. Borrow it instead of placing a hold.");
        }

        Hold hold = holdRepository.save(new Hold(book, member, LocalDateTime.now(clock)));
        long position = holdRepository.queuePosition(bookId, hold.getPlacedAt(), hold.getId());
        auditService.record(member, AuditAction.HOLD_PLACED, "Hold", hold.getId(),
                "placed a hold on \"" + book.getTitle() + "\" (position " + position + ")");
        return HoldResponse.from(hold, position);
    }

    /** Members cancel their own holds; staff can cancel any. A READY hold releases its copy to the next in line. */
    @Transactional
    public HoldResponse cancel(Long holdId, User actor) {
        Hold hold = find(holdId);
        if (!actor.isStaff() && !hold.getMember().getId().equals(actor.getId())) {
            throw new AccessDeniedException("This hold belongs to another member");
        }
        if (!hold.getStatus().isActive()) {
            throw new BadRequestException("This hold is already " + hold.getStatus().name().toLowerCase());
        }
        Copy released = hold.getStatus() == HoldStatus.READY ? hold.getCopy() : null;
        hold.close(HoldStatus.CANCELLED, LocalDateTime.now(clock));
        auditService.record(actor, AuditAction.HOLD_CANCELLED, "Hold", hold.getId(),
                hold.getMember().getId().equals(actor.getId())
                        ? "cancelled their hold on \"" + hold.getBook().getTitle() + "\""
                        : "cancelled the hold on \"" + hold.getBook().getTitle() + "\" for " + hold.getMember().getName());
        if (released != null) {
            allocate(released, actor);
        }
        return HoldResponse.from(hold, null);
    }

    /**
     * Offers a free copy to the first member waiting for its title. If someone is waiting the copy goes
     * to the hold shelf and they are notified; otherwise it goes back on the open shelf.
     *
     * @return the hold that is now READY, if any
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Hold> allocate(Copy copy, User actor) {
        LocalDate today = LocalDate.now(clock);
        Optional<Hold> next = holdRepository.findFirstByBookIdAndStatusOrderByPlacedAtAscIdAsc(
                copy.getBook().getId(), HoldStatus.WAITING);
        if (next.isEmpty()) {
            copy.setStatus(CopyStatus.AVAILABLE);
            return Optional.empty();
        }
        Hold hold = next.get();
        hold.markReady(copy, today, settingsService.current().getHoldPickupDays());
        copy.setStatus(CopyStatus.ON_HOLD_SHELF);
        notificationService.holdReady(hold);
        auditService.record(actor, AuditAction.HOLD_READY, "Hold", hold.getId(),
                "Copy " + copy.getBarcode() + " of \"" + copy.getBook().getTitle() + "\" set aside for "
                        + hold.getMember().getName() + " until " + Dates.human(hold.getExpiresOn()));
        return Optional.of(hold);
    }

    /** Called when a member borrows a title they were holding: the hold has done its job. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void fulfil(Hold hold) {
        hold.close(HoldStatus.FULFILLED, LocalDateTime.now(clock));
    }

    /** READY holds past their pickup date expire, and each copy moves on to the next member in line. */
    @Transactional
    public int expireUncollected() {
        LocalDate today = LocalDate.now(clock);
        List<Hold> expired = holdRepository.findByStatusAndExpiresOnBefore(HoldStatus.READY, today);
        for (Hold hold : expired) {
            Copy copy = hold.getCopy();
            hold.close(HoldStatus.EXPIRED, LocalDateTime.now(clock));
            auditService.record(null, AuditAction.HOLD_EXPIRED, "Hold", hold.getId(),
                    hold.getMember().getName() + " did not collect \"" + hold.getBook().getTitle() + "\" by " + Dates.human(hold.getExpiresOn()));
            if (copy != null) {
                allocate(copy, null);
            }
        }
        return expired.size();
    }

    // ------------------------------------------------------------------ queries

    @Transactional(readOnly = true)
    public List<HoldResponse> forMember(Long memberId) {
        return holdRepository.findByMemberIdOrderByPlacedAtDesc(memberId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<HoldResponse> activeForMember(Long memberId) {
        return holdRepository.findByMemberIdAndStatusInOrderByPlacedAtAsc(memberId, ACTIVE).stream()
                .map(this::toResponse).toList();
    }

    /** @param status waiting | ready | active (default) */
    @Transactional(readOnly = true)
    public List<HoldResponse> list(String status) {
        Set<HoldStatus> statuses = switch (status == null ? "active" : status.toLowerCase()) {
            case "waiting" -> EnumSet.of(HoldStatus.WAITING);
            case "ready" -> EnumSet.of(HoldStatus.READY);
            case "active" -> ACTIVE;
            default -> throw new BadRequestException("Unknown status: " + status);
        };
        return holdRepository.findByStatusInOrderByPlacedAtAsc(statuses).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<HoldResponse> queueForBook(Long bookId) {
        return holdRepository.findByBookIdAndStatusInOrderByPlacedAtAsc(bookId, ACTIVE).stream()
                .map(this::toResponse).toList();
    }

    private HoldResponse toResponse(Hold h) {
        Long position = h.getStatus() == HoldStatus.WAITING
                ? holdRepository.queuePosition(h.getBook().getId(), h.getPlacedAt(), h.getId())
                : null;
        return HoldResponse.from(h, position);
    }

    private Hold find(Long id) {
        return holdRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Hold", id));
    }
}
