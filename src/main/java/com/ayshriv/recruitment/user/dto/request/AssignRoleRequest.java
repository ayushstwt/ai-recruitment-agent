package com.ayshriv.recruitment.user.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Request payload for assigning roles to a user.
 *
 * <p>Every requested role is validated against the user's organization:
 * organization roles must belong to the same tenant and system roles are
 * shared globally. Duplicate ids are ignored.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssignRoleRequest {

    /**
     * Role ids to assign.
     */
    @NotNull(message = "Role ids are required")
    @NotEmpty(message = "At least one role id is required")
    @Size(max = 20, message = "At most 20 roles can be assigned at once")
    private List<Long> roleIds;
}