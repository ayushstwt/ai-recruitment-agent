package com.ayshriv.recruitment.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request is made without valid authentication credentials.
 */
public class UnauthorizedException extends ApiException {

    /**
     * Create an unauthorized exception.
     *
     * @param message human readable error message
     */
    public UnauthorizedException(String message) {
        super(message, "UNAUTHORIZED", HttpStatus.UNAUTHORIZED);
    }

    /**
     * Create an unauthorized exception with a custom error code.
     *
     * @param message human readable error message
     * @param code    machine readable error code
     */
    public UnauthorizedException(String message, String code) {
        super(message, code, HttpStatus.UNAUTHORIZED);
    }
}