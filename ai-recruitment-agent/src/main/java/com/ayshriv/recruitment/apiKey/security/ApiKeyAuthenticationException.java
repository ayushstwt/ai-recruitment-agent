package com.ayshriv.recruitment.apiKey.security;

import org.springframework.security.core.AuthenticationException;

/**
 * Thrown when an API key cannot be authenticated.
 *
 * <p>Carries a machine readable {@code code} so the entry point can return
 * a consistent error envelope. Valid codes are {@code INVALID_API_KEY},
 * {@code API_KEY_INACTIVE}, {@code API_KEY_EXPIRED} and
 * {@code API_KEY_REQUIRED}.</p>
 */
public class ApiKeyAuthenticationException extends AuthenticationException {

    private final String code;

    /**
     * Create an API key authentication failure.
     *
     * @param message human readable error message
     * @param code    machine readable error code
     */
    public ApiKeyAuthenticationException(String message, String code) {
        super(message);
        this.code = code;
    }

    /**
     * Machine readable error code.
     *
     * @return error code
     */
    public String getCode() {
        return code;
    }
}