package com.library.member;

/** A suspended member can still log in and see their account, but cannot borrow, renew or place holds. */
public enum AccountStatus {
    ACTIVE,
    SUSPENDED
}
