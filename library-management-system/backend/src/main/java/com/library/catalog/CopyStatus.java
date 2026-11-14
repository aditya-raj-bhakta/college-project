package com.library.catalog;

/**
 * Where a physical copy is.
 *
 *   AVAILABLE      on the open shelf, anyone eligible can borrow it
 *   ON_LOAN        checked out to a member
 *   ON_HOLD_SHELF  returned and set aside for the member at the front of the holds queue
 *   DAMAGED        out of circulation until repaired
 *   LOST           declared lost; the member was charged
 *   WITHDRAWN      removed from the collection for good
 */
public enum CopyStatus {
    AVAILABLE,
    ON_LOAN,
    ON_HOLD_SHELF,
    DAMAGED,
    LOST,
    WITHDRAWN;

    /** Copies that count towards a title's stock: on the shelf, out, or waiting for pickup. */
    public boolean isCirculating() {
        return this == AVAILABLE || this == ON_LOAN || this == ON_HOLD_SHELF;
    }

    /** Statuses staff may set by hand. ON_LOAN and ON_HOLD_SHELF only change through circulation. */
    public boolean isManuallySettable() {
        return this == AVAILABLE || this == DAMAGED || this == LOST || this == WITHDRAWN;
    }
}
