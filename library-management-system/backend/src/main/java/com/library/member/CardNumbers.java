package com.library.member;

/** Library card numbers are M + the member's id, zero padded, e.g. M000004. */
public final class CardNumbers {

    private CardNumbers() {
    }

    public static String forMember(Long id) {
        return String.format("M%06d", id);
    }
}
