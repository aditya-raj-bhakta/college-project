package com.library.common.exception;

/** A request that breaks a library rule (no copies left, loan limit reached, and so on). */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
