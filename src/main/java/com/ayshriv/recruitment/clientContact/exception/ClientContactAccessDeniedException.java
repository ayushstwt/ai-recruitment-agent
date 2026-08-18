package com.ayshriv.recruitment.clientContact.exception;

import com.ayshriv.recruitment.common.exception.ForbiddenException;

/**
 * Thrown when an authenticated organization attempts to access a client
 * contact that belongs to another organization or to a client it does not own.
 */
public class ClientContactAccessDeniedException extends ForbiddenException {

    /**
     * Create a client contact access denied exception.
     */
    public ClientContactAccessDeniedException() {
        super("Access denied to the requested client contact", "CLIENT_CONTACT_ACCESS_DENIED");
    }
}