package com.library.circulation;

import com.library.catalog.Copy;
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

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** One copy checked out to one member, from issue to check-in. */
@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "copy_id", nullable = false)
    private Copy copy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    @Column(nullable = false)
    private LocalDate borrowDate;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDate returnDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanState state = LoanState.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoanChannel channel;

    @Column(nullable = false)
    private int renewalCount = 0;

    /** The librarian who issued it at the desk; null for self-service loans. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issued_by_id")
    private User issuedBy;

    /** The librarian who checked it back in. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by_id")
    private User receivedBy;

    protected Loan() {
    }

    public Loan(Copy copy, User member, LocalDate borrowDate, LocalDate dueDate, LoanChannel channel, User issuedBy) {
        this.copy = copy;
        this.member = member;
        this.borrowDate = borrowDate;
        this.dueDate = dueDate;
        this.channel = channel;
        this.issuedBy = issuedBy;
    }

    public boolean isOpen() {
        return state == LoanState.OPEN;
    }

    public boolean isOverdue(LocalDate today) {
        return isOpen() && today.isAfter(dueDate);
    }

    public long daysOverdue(LocalDate today) {
        LocalDate end = isOpen() ? today : (returnDate != null ? returnDate : today);
        long days = ChronoUnit.DAYS.between(dueDate, end);
        return Math.max(days, 0);
    }

    public LoanStatus getStatus(LocalDate today) {
        return switch (state) {
            case RETURNED -> LoanStatus.RETURNED;
            case LOST -> LoanStatus.LOST;
            case OPEN -> isOverdue(today) ? LoanStatus.OVERDUE : LoanStatus.ACTIVE;
        };
    }

    void checkIn(LocalDate today, User librarian) {
        this.state = LoanState.RETURNED;
        this.returnDate = today;
        this.receivedBy = librarian;
    }

    void markLost(LocalDate today, User librarian) {
        this.state = LoanState.LOST;
        this.returnDate = today;
        this.receivedBy = librarian;
    }

    void renew(LocalDate newDueDate) {
        this.dueDate = newDueDate;
        this.renewalCount++;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Copy getCopy() { return copy; }
    public User getMember() { return member; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public LoanState getState() { return state; }
    public void setState(LoanState state) { this.state = state; }
    public LoanChannel getChannel() { return channel; }
    public int getRenewalCount() { return renewalCount; }
    public User getIssuedBy() { return issuedBy; }
    public User getReceivedBy() { return receivedBy; }
}
