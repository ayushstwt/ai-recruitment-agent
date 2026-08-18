package com.ayshriv.recruitment.client.exception;

import com.ayshriv.recruitment.common.exception.ForbiddenException;

/**
 * Thrown when an authenticated organization attempts to access a client that
 * belongs to another organization.
 */
public class ClientAccessDeniedException extends ForbiddenException {

    /**
     * Create a client access denied exception.
     */
    public ClientAccessDeniedException() {
        super("Access denied to the requested client", "CLIENT_ACCESS_DENIED");
    }
}