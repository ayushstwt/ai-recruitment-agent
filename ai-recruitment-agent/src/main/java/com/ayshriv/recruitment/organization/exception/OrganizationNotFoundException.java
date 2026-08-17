package com.ayshriv.recruitment.organization.exception;

import com.ayshriv.recruitment.common.exception.ResourceNotFoundException;

/**
 * Thrown when an organization cannot be found because it does not exist or
 * has been soft deleted.
 */
public class OrganizationNotFoundException extends ResourceNotFoundException {

    /**
     * Create a not found exception for an organization.
     *
     * @param id organization primary key
     */
    public OrganizationNotFoundException(Long id) {
        super("Organization not found with id: " + id, "ORGANIZATION_NOT_FOUND");
    }
}
