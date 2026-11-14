package com.library.hold;

import com.library.catalog.Book;
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
import java.time.LocalDateTime;

/** A member's place in the queue for a title. */
@Entity
@Table(name = "holds")
public class Hold {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private User member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private HoldStatus status = HoldStatus.WAITING;

    @Column(nullable = false)
    private LocalDateTime placedAt;

    /** The copy set aside once the hold is READY. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "copy_id")
    private Copy copy;

    private LocalDate readyOn;

    private LocalDate expiresOn;

    private LocalDateTime closedAt;

    protected Hold() {
    }

    public Hold(Book book, User member, LocalDateTime placedAt) {
        this.book = book;
        this.member = member;
        this.placedAt = placedAt;
    }

    void markReady(Copy copy, LocalDate today, int pickupDays) {
        this.status = HoldStatus.READY;
        this.copy = copy;
        this.readyOn = today;
        this.expiresOn = today.plusDays(pickupDays);
    }

    void close(HoldStatus finalStatus, LocalDateTime when) {
        this.status = finalStatus;
        this.closedAt = when;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Book getBook() { return book; }
    public User getMember() { return member; }
    public HoldStatus getStatus() { return status; }
    public LocalDateTime getPlacedAt() { return placedAt; }
    public Copy getCopy() { return copy; }
    public LocalDate getReadyOn() { return readyOn; }
    public LocalDate getExpiresOn() { return expiresOn; }
    public LocalDateTime getClosedAt() { return closedAt; }
}
