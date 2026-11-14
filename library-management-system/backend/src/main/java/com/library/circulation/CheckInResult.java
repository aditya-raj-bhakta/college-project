package com.library.circulation;

import java.time.LocalDate;

/**
 * What the librarian needs to know after scanning a returned copy.
 *
 * @param fineCharged late fine added to the member's account, 0 if on time
 * @param holdFor     if someone is waiting, the copy goes to the hold shelf for them; otherwise null
 */
public record CheckInResult(
        LoanResponse loan,
        int fineCharged,
        HoldNotice holdFor,
        String shelfLocation
) {
    public record HoldNotice(Long holdId, String memberName, String cardNumber, LocalDate expiresOn) {
    }
}
