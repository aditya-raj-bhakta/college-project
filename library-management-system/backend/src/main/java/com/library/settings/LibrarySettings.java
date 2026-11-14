package com.library.settings;

import com.library.member.MemberType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Library-wide settings. There is always exactly one row, with id 1. */
@Entity
@Table(name = "library_settings")
public class LibrarySettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    /** Days a returned copy waits on the hold shelf for the member who reserved it. */
    @Column(nullable = false)
    private int holdPickupDays;

    /** Charged when a copy is declared lost, on top of any late fine. */
    @Column(nullable = false)
    private int lostItemFee;

    /** Due-soon reminders go out this many days before the due date. */
    @Column(nullable = false)
    private int reminderDaysBefore;

    /** Given to members who register themselves. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "default_member_type_id", nullable = false)
    private MemberType defaultMemberType;

    protected LibrarySettings() {
    }

    public Long getId() { return id; }
    public int getHoldPickupDays() { return holdPickupDays; }
    public void setHoldPickupDays(int holdPickupDays) { this.holdPickupDays = holdPickupDays; }
    public int getLostItemFee() { return lostItemFee; }
    public void setLostItemFee(int lostItemFee) { this.lostItemFee = lostItemFee; }
    public int getReminderDaysBefore() { return reminderDaysBefore; }
    public void setReminderDaysBefore(int reminderDaysBefore) { this.reminderDaysBefore = reminderDaysBefore; }
    public MemberType getDefaultMemberType() { return defaultMemberType; }
    public void setDefaultMemberType(MemberType defaultMemberType) { this.defaultMemberType = defaultMemberType; }
}
