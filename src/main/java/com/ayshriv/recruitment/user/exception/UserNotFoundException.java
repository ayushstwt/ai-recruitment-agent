package com.ayshriv.recruitment.user.exception;

import com.ayshriv.recruitment.common.exception.ResourceNotFoundException;

/**
 * Thrown when a user cannot be found because it does not exist or has been
 * soft deleted.
 */
public class UserNotFoundException extends ResourceNotFoundException {

    /**
     * Create a not found exception for a user.
     *
     * @param id user primary key
     */
    public UserNotFoundException(Long id) {
        super("User not found with id: " + id, "USER_NOT_FOUND");
    }
}