package com.ayshriv.recruitment.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource cannot be found.
 */
public class ResourceNotFoundException extends ApiException {

    /**
     * Create a not found exception.
     *
     * @param message human readable error message
     */
    public ResourceNotFoundException(String message) {
        super(message, "RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND);
    }

    /**
     * Create a not found exception with a custom error code.
     *
     * @param message human readable error message
     * @param code    machine readable error code, for example {@code CANDIDATE_NOT_FOUND}
     */
    public ResourceNotFoundException(String message, String code) {
        super(message, code, HttpStatus.NOT_FOUND);
    }
}