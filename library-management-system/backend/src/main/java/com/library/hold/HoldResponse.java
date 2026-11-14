package com.library.hold;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @param queuePosition 1 = next in line; null unless the hold is WAITING
 * @param barcode       the copy set aside for a READY hold
 */
public record HoldResponse(
        Long id,
        Long bookId,
        String bookTitle,
        String bookAuthor,
        Long memberId,
        String memberName,
        String cardNumber,
        HoldStatus status,
        LocalDateTime placedAt,
        Long queuePosition,
        String barcode,
        LocalDate readyOn,
        LocalDate expiresOn,
        LocalDateTime closedAt
) {
    public static HoldResponse from(Hold h, Long queuePosition) {
        return new HoldResponse(h.getId(), h.getBook().getId(), h.getBook().getTitle(), h.getBook().getAuthor(),
                h.getMember().getId(), h.getMember().getName(), h.getMember().getCardNumber(),
                h.getStatus(), h.getPlacedAt(), queuePosition,
                h.getCopy() == null ? null : h.getCopy().getBarcode(),
                h.getReadyOn(), h.getExpiresOn(), h.getClosedAt());
    }
}
