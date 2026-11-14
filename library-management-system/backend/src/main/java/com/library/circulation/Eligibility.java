package com.library.circulation;

import java.util.List;

/**
 * Whether a member may borrow right now, and if not, every reason why.
 * Shown to the member on their dashboard and to the librarian at the desk.
 */
public record Eligibility(
        boolean canBorrow,
        List<String> blocks,
        long openLoans,
        int maxLoans,
        long outstandingFines,
        int fineBlockThreshold
) {
}
