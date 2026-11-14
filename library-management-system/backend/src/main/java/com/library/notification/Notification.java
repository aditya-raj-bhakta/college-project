package com.library.notification;

import com.library.circulation.Loan;
import com.library.hold.Hold;
import com.library.member.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** A message sent to a member. Kept so members can see it and so nothing is sent twice. */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationKind kind;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id")
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hold_id")
    private Hold hold;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    protected Notification() {
    }

    public Notification(User member, NotificationKind kind, Loan loan, Hold hold, String message, LocalDateTime sentAt) {
        this.member = member;
        this.kind = kind;
        this.loan = loan;
        this.hold = hold;
        this.message = message;
        this.sentAt = sentAt;
    }

    public Long getId() { return id; }
    public User getMember() { return member; }
    public NotificationKind getKind() { return kind; }
    public Loan getLoan() { return loan; }
    public Hold getHold() { return hold; }
    public String getMessage() { return message; }
    public LocalDateTime getSentAt() { return sentAt; }
}
