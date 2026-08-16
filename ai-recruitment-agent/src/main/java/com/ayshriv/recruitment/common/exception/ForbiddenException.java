package com.ayshriv.recruitment.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an authenticated user attempts an action they are not
 * allowed to perform.
 */
public class ForbiddenException extends ApiException {

    /**
     * Create a forbidden exception.
     *
     * @param message human readable error message
     */
    public ForbiddenException(String message) {
        super(message, "FORBIDDEN", HttpStatus.FORBIDDEN);
    }

    /**
     * Create a forbidden exception with a custom error code.
     *
     * @param message human readable error message
     * @param code    machine readable error code
     */
    public ForbiddenException(String message, String code) {
        super(message, code, HttpStatus.FORBIDDEN);
    }
}