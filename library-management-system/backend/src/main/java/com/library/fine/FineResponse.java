package com.library.fine;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record FineResponse(
        Long id,
        Long memberId,
        String memberName,
        String cardNumber,
        Long loanId,
        String bookTitle,
        FineType type,
        String description,
        int amount,
        int paid,
        int waived,
        int balance,
        boolean settled,
        LocalDate createdOn,
        List<Transaction> transactions
) {
    public record Transaction(Long id, TransactionKind kind, int amount, String note, String staffName, LocalDateTime createdAt) {
        static Transaction from(FineTransaction t) {
            return new Transaction(t.getId(), t.getKind(), t.getAmount(), t.getNote(), t.getStaff().getName(), t.getCreatedAt());
        }
    }

    static FineResponse from(Fine f, List<FineTransaction> transactions) {
        return new FineResponse(f.getId(), f.getMember().getId(), f.getMember().getName(), f.getMember().getCardNumber(),
                f.getLoan() == null ? null : f.getLoan().getId(),
                f.getLoan() == null ? null : f.getLoan().getCopy().getBook().getTitle(),
                f.getType(), f.getDescription(), f.getAmount(), f.getPaid(), f.getWaived(), f.getBalance(), f.isSettled(),
                f.getCreatedOn(), transactions.stream().map(Transaction::from).toList());
    }
}
