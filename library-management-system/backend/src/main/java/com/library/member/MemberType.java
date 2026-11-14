package com.library.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Borrowing rules for a group of members, such as students or faculty. */
@Entity
@Table(name = "member_types")
public class MemberType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 60)
    private String name;

    /** Most books a member can have out at once. */
    @Column(nullable = false)
    private int maxLoans;

    @Column(nullable = false)
    private int loanDays;

    @Column(nullable = false)
    private int maxRenewals;

    /** Rupees per day late. */
    @Column(nullable = false)
    private int finePerDay;

    /** Unpaid fines above this amount block borrowing, renewing and holds. */
    @Column(nullable = false)
    private int fineBlockThreshold;

    @Column(nullable = false)
    private int maxHolds;

    protected MemberType() {
    }

    public MemberType(String code, String name, int maxLoans, int loanDays, int maxRenewals,
                      int finePerDay, int fineBlockThreshold, int maxHolds) {
        this.code = code;
        this.name = name;
        this.maxLoans = maxLoans;
        this.loanDays = loanDays;
        this.maxRenewals = maxRenewals;
        this.finePerDay = finePerDay;
        this.fineBlockThreshold = fineBlockThreshold;
        this.maxHolds = maxHolds;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getMaxLoans() { return maxLoans; }
    public void setMaxLoans(int maxLoans) { this.maxLoans = maxLoans; }
    public int getLoanDays() { return loanDays; }
    public void setLoanDays(int loanDays) { this.loanDays = loanDays; }
    public int getMaxRenewals() { return maxRenewals; }
    public void setMaxRenewals(int maxRenewals) { this.maxRenewals = maxRenewals; }
    public int getFinePerDay() { return finePerDay; }
    public void setFinePerDay(int finePerDay) { this.finePerDay = finePerDay; }
    public int getFineBlockThreshold() { return fineBlockThreshold; }
    public void setFineBlockThreshold(int fineBlockThreshold) { this.fineBlockThreshold = fineBlockThreshold; }
    public int getMaxHolds() { return maxHolds; }
    public void setMaxHolds(int maxHolds) { this.maxHolds = maxHolds; }
}
