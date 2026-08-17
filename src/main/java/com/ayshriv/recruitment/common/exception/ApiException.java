package com.ayshriv.recruitment.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for all business exceptions.
 *
 * <p>Carries a machine readable {@code code} and the HTTP status that
 * should be returned to the caller when the exception is thrown.</p>
 */
public abstract class ApiException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    /**
     * Create a business exception.
     *
     * @param message human readable error message
     * @param code    machine readable error code
     * @param status  HTTP status to return
     */
    protected ApiException(String message, String code, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    /**
     * Create a business exception with a cause.
     *
     * @param message human readable error message
     * @param code    machine readable error code
     * @param status  HTTP status to return
     * @param cause   underlying cause
     */
    protected ApiException(String message, String code, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.status = status;
    }

    /**
     * Machine readable error code.
     *
     * @return error code
     */
    public String getCode() {
        return code;
    }

    /**
     * HTTP status to return.
     *
     * @return HTTP status
     */
    public HttpStatus getStatus() {
        return status;
    }
}