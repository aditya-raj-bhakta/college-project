package com.library.member;

import java.time.LocalDateTime;

/** A row on the Members page. */
public record MemberSummary(
        Long id,
        String name,
        String email,
        String cardNumber,
        String memberTypeName,
        AccountStatus status,
        long openLoans,
        long overdueLoans,
        long outstandingFines,
        long totalLoans,
        LocalDateTime createdAt
) {
}
