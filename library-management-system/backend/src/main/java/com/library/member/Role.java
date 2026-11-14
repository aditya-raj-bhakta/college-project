package com.library.member;

/**
 * MEMBER borrows. LIBRARIAN runs the circulation desk and the catalogue.
 * ADMIN does everything a librarian does, plus rules, staff accounts and the audit log.
 */
public enum Role {
    MEMBER,
    LIBRARIAN,
    ADMIN;

    public boolean isStaff() {
        return this == LIBRARIAN || this == ADMIN;
    }
}
