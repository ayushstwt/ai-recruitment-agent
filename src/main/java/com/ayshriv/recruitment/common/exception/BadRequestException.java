package com.ayshriv.recruitment.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request contains invalid or malformed data.
 */
public class BadRequestException extends ApiException {

    /**
     * Create a bad request exception.
     *
     * @param message human readable error message
     */
    public BadRequestException(String message) {
        super(message, "BAD_REQUEST", HttpStatus.BAD_REQUEST);
    }

    /**
     * Create a bad request exception with a custom error code.
     *
     * @param message human readable error message
     * @param code    machine readable error code
     */
    public BadRequestException(String message, String code) {
        super(message, code, HttpStatus.BAD_REQUEST);
    }
}