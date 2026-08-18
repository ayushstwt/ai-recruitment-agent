package com.ayshriv.recruitment.clientContact.exception;

import com.ayshriv.recruitment.common.exception.ResourceNotFoundException;

/**
 * Thrown when a client contact cannot be found because it does not exist or
 * has been soft deleted.
 */
public class ClientContactNotFoundException extends ResourceNotFoundException {

    /**
     * Create a not found exception for a client contact.
     *
     * @param id contact primary key
     */
    public ClientContactNotFoundException(Long id) {
        super("Client contact not found with id: " + id, "CLIENT_CONTACT_NOT_FOUND");
    }
}