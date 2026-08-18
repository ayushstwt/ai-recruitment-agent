package com.ayshriv.recruitment.client.exception;

import com.ayshriv.recruitment.common.exception.DuplicateResourceException;

/**
 * Thrown when a client would be created or updated with an email or client
 * code that is already used by another client of the same organization.
 */
public class DuplicateClientException extends DuplicateResourceException {

    /**
     * Create a duplicate client exception.
     *
     * @param message human readable conflict message
     */
    public DuplicateClientException(String message) {
        super(message, "CLIENT_ALREADY_EXISTS");
    }

    /**
     * Create a duplicate client exception for a conflicting email.
     *
     * @param email conflicting email address
     */
    public static DuplicateClientException forEmail(String email) {
        return new DuplicateClientException(
                "A client with email " + email + " already exists in this organization");
    }

    /**
     * Create a duplicate client exception for a client code allocation
     * conflict.
     */
    public static DuplicateClientException forClientCodeConflict() {
        return new DuplicateClientException(
                "A unique client code could not be allocated, please retry");
    }
}