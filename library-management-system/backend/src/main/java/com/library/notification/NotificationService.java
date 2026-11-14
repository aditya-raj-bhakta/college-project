package com.library.notification;

import com.library.circulation.Loan;
import com.library.hold.Hold;
import com.library.member.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Writes member messages and hands them to the {@link NotificationSender}. Each loan reminder goes out once. */
@Service
public class NotificationService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM yyyy");

    private final NotificationRepository repository;
    private final NotificationSender sender;
    private final Clock clock;

    public NotificationService(NotificationRepository repository, NotificationSender sender, Clock clock) {
        this.repository = repository;
        this.sender = sender;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void holdReady(Hold hold) {
        String message = "\"" + hold.getBook().getTitle() + "\" is waiting for you at the desk. "
                + "Pick it up by " + DATE.format(hold.getExpiresOn()) + ".";
        deliver(hold.getMember(), NotificationKind.HOLD_READY, null, hold, "Your hold is ready", message);
    }

    /** @return true if a reminder was sent, false if this loan already had one */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean dueSoon(Loan loan) {
        if (repository.existsByLoanIdAndKind(loan.getId(), NotificationKind.DUE_SOON)) {
            return false;
        }
        String message = "\"" + loan.getCopy().getBook().getTitle() + "\" is due back on "
                + DATE.format(loan.getDueDate()) + ".";
        deliver(loan.getMember(), NotificationKind.DUE_SOON, loan, null, "Due soon", message);
        return true;
    }

    /** @return true if a notice was sent, false if this loan already had one */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean overdue(Loan loan, int finePerDay) {
        if (repository.existsByLoanIdAndKind(loan.getId(), NotificationKind.OVERDUE)) {
            return false;
        }
        String message = "\"" + loan.getCopy().getBook().getTitle() + "\" was due on "
                + DATE.format(loan.getDueDate()) + ". Please return it at the desk. "
                + "The fine is ₹" + finePerDay + " a day.";
        deliver(loan.getMember(), NotificationKind.OVERDUE, loan, null, "Overdue", message);
        return true;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> recentFor(Long memberId) {
        return repository.findTop20ByMemberIdOrderBySentAtDesc(memberId).stream()
                .map(NotificationResponse::from).toList();
    }

    private void deliver(User member, NotificationKind kind, Loan loan, Hold hold, String subject, String message) {
        repository.save(new Notification(member, kind, loan, hold, message, LocalDateTime.now(clock)));
        sender.send(member.getName(), member.getEmail(), subject, message);
    }
}
