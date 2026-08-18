package com.ayshriv.recruitment.client.exception;

import com.ayshriv.recruitment.common.exception.ResourceNotFoundException;

/**
 * Thrown when a client cannot be found because it does not exist or has been
 * soft deleted.
 */
public class ClientNotFoundException extends ResourceNotFoundException {

    /**
     * Create a not found exception for a client.
     *
     * @param id client primary key
     */
    public ClientNotFoundException(Long id) {
        super("Client not found with id: " + id, "CLIENT_NOT_FOUND");
    }
}