package com.library.hold;

/**
 *   WAITING    in the queue for the next copy
 *   READY      a copy is on the hold shelf for this member until expiresOn
 *   FULFILLED  the member borrowed the held copy
 *   CANCELLED  withdrawn by the member or staff
 *   EXPIRED    not picked up in time; the copy went to the next in line
 */
public enum HoldStatus {
    WAITING,
    READY,
    FULFILLED,
    CANCELLED,
    EXPIRED;

    public boolean isActive() {
        return this == WAITING || this == READY;
    }
}
