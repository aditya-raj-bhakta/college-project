package com.library.member;

import java.time.LocalDateTime;

/** The logged-in user, as the frontend needs it. */
public record UserResponse(
        Long id,
        String name,
        String email,
        Role role,
        String cardNumber,
        AccountStatus status,
        String suspendedReason,
        MemberTypeResponse memberType,
        LocalDateTime createdAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getCardNumber(), u.getStatus(),
                u.getSuspendedReason(), MemberTypeResponse.from(u.getMemberType()), u.getCreatedAt());
    }
}
