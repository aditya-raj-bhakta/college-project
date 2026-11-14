package com.library.catalog;

import java.time.LocalDate;

/**
 * @param borrowerName for ON_LOAN copies, who has it
 * @param heldForName  for ON_HOLD_SHELF copies, who it is waiting for
 */
public record CopyResponse(
        Long id,
        Long bookId,
        String bookTitle,
        String barcode,
        String shelfLocation,
        CopyStatus status,
        String note,
        LocalDate acquiredOn,
        String borrowerName,
        LocalDate dueDate,
        String heldForName,
        LocalDate holdExpiresOn
) {
}
