package com.library.circulation;

import java.time.LocalDate;

/**
 * @param accruingFine  for an open overdue loan, what the fine would be if it came back today
 * @param renewBlockedReason null when the loan can be renewed; only filled in for member views
 */
public record LoanResponse(
        Long id,
        Long bookId,
        String bookTitle,
        String bookAuthor,
        Long copyId,
        String barcode,
        Long memberId,
        String memberName,
        String memberEmail,
        String cardNumber,
        LocalDate borrowDate,
        LocalDate dueDate,
        LocalDate returnDate,
        LoanStatus status,
        long daysOverdue,
        int renewalCount,
        LoanChannel channel,
        int accruingFine,
        Boolean canRenew,
        String renewBlockedReason
) {
    public static LoanResponse from(Loan l, LocalDate today) {
        return from(l, today, null);
    }

    static LoanResponse from(Loan l, LocalDate today, String renewBlockedReason) {
        LoanStatus status = l.getStatus(today);
        long overdue = status == LoanStatus.OVERDUE ? l.daysOverdue(today) : 0;
        int rate = l.getMember().getMemberType() == null ? 0 : l.getMember().getMemberType().getFinePerDay();
        return new LoanResponse(l.getId(), l.getCopy().getBook().getId(), l.getCopy().getBook().getTitle(),
                l.getCopy().getBook().getAuthor(), l.getCopy().getId(), l.getCopy().getBarcode(),
                l.getMember().getId(), l.getMember().getName(), l.getMember().getEmail(), l.getMember().getCardNumber(),
                l.getBorrowDate(), l.getDueDate(), l.getReturnDate(), status, overdue, l.getRenewalCount(), l.getChannel(),
                (int) overdue * rate,
                renewBlockedReason == null && l.isOpen() ? Boolean.TRUE : (l.isOpen() ? Boolean.FALSE : null),
                renewBlockedReason);
    }
}
