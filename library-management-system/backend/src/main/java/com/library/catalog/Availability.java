package com.library.catalog;

/**
 * Stock for one title.
 *
 * @param total        copies in circulation (on the shelf, out, or on the hold shelf)
 * @param available    on the open shelf now
 * @param waitingHolds members queued for the next copy
 */
public record Availability(int total, int available, int onLoan, int onHoldShelf, int waitingHolds) {

    public static final Availability NONE = new Availability(0, 0, 0, 0, 0);
}
