package com.ayshriv.recruitment.user.exception;

import com.ayshriv.recruitment.common.exception.DuplicateResourceException;

/**
 * Thrown when a user would be created or updated with an email that is
 * already used by another user of the same organization.
 */
public class DuplicateUserException extends DuplicateResourceException {

    /**
     * Create a duplicate user exception.
     *
     * @param email conflicting email address
     */
    public DuplicateUserException(String email) {
        super("A user with email " + email + " already exists in this organization",
                "USER_ALREADY_EXISTS");
    }
}