package com.library.fine;

import com.library.circulation.Loan;
import com.library.common.exception.BadRequestException;
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
import jakarta.persistence.Version;

import java.time.LocalDate;

/**
 * A charge on a member's account. Payments and waivers reduce the balance and are
 * recorded one by one as {@link FineTransaction}s, so nothing is ever edited in place.
 */
@Entity
@Table(name = "fines")
public class Fine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id")
    private Loan loan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FineType type;

    /** Rupees charged. */
    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private int paid = 0;

    @Column(nullable = false)
    private int waived = 0;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDate createdOn;

    @Version
    private Long version;

    protected Fine() {
    }

    public Fine(User member, Loan loan, FineType type, int amount, String description, LocalDate createdOn) {
        if (amount <= 0) {
            throw new IllegalArgumentException("A fine must be a positive amount");
        }
        this.member = member;
        this.loan = loan;
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.createdOn = createdOn;
    }

    public int getBalance() {
        return amount - paid - waived;
    }

    public boolean isSettled() {
        return getBalance() == 0;
    }

    void applyPayment(int value) {
        requireWithinBalance(value);
        this.paid += value;
    }

    void applyWaiver(int value) {
        requireWithinBalance(value);
        this.waived += value;
    }

    private void requireWithinBalance(int value) {
        if (value <= 0) {
            throw new BadRequestException("Enter an amount greater than zero");
        }
        if (value > getBalance()) {
            throw new BadRequestException("The amount is more than the ₹" + getBalance() + " still owed");
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getMember() { return member; }
    public Loan getLoan() { return loan; }
    public FineType getType() { return type; }
    public int getAmount() { return amount; }
    public int getPaid() { return paid; }
    public int getWaived() { return waived; }
    public String getDescription() { return description; }
    public LocalDate getCreatedOn() { return createdOn; }
}
