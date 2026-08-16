package com.ayshriv.recruitment.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an operation would create a resource that already exists,
 * violating a uniqueness constraint.
 */
public class DuplicateResourceException extends ApiException {

    /**
     * Create a duplicate resource exception.
     *
     * @param message human readable error message
     */
    public DuplicateResourceException(String message) {
        super(message, "DUPLICATE_RESOURCE", HttpStatus.CONFLICT);
    }

    /**
     * Create a duplicate resource exception with a custom error code.
     *
     * @param message human readable error message
     * @param code    machine readable error code, for example {@code CANDIDATE_ALREADY_EXISTS}
     */
    public DuplicateResourceException(String message, String code) {
        super(message, code, HttpStatus.CONFLICT);
    }
}