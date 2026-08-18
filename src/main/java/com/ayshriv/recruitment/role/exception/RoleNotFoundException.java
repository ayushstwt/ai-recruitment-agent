package com.ayshriv.recruitment.role.exception;

import com.ayshriv.recruitment.common.exception.ResourceNotFoundException;

/**
 * Thrown when a role cannot be found because it does not exist, has been soft
 * deleted, or is not accessible to the requesting tenant.
 */
public class RoleNotFoundException extends ResourceNotFoundException {

    /**
     * Create a not found exception for a role.
     *
     * @param id role primary key
     */
    public RoleNotFoundException(Long id) {
        super("Role not found with id: " + id, "ROLE_NOT_FOUND");
    }
}