package com.library.fine;

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

/** One payment or waiver against a fine, with the librarian who took it. */
@Entity
@Table(name = "fine_transactions")
public class FineTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fine_id", nullable = false)
    private Fine fine;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionKind kind;

    @Column(nullable = false)
    private int amount;

    private String note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "staff_id", nullable = false)
    private User staff;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected FineTransaction() {
    }

    public FineTransaction(Fine fine, TransactionKind kind, int amount, String note, User staff, LocalDateTime createdAt) {
        this.fine = fine;
        this.kind = kind;
        this.amount = amount;
        this.note = note;
        this.staff = staff;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Fine getFine() { return fine; }
    public TransactionKind getKind() { return kind; }
    public int getAmount() { return amount; }
    public String getNote() { return note; }
    public User getStaff() { return staff; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
