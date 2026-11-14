package com.library.circulation;

/** Stored state of a loan. "Overdue" is not stored: it is an OPEN loan past its due date. */
public enum LoanState {
    OPEN,
    RETURNED,
    LOST
}
