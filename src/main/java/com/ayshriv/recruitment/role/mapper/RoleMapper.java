package com.ayshriv.recruitment.role.mapper;

import com.ayshriv.recruitment.role.dto.response.RoleResponse;
import com.ayshriv.recruitment.role.entity.Role;
import org.springframework.stereotype.Component;

/**
 * Converts between the role JPA entity and its response DTO.
 *
 * <p>Controllers and services never map entities themselves; the role module
 * is the only place that knows how a {@link Role} becomes a
 * {@link RoleResponse}.</p>
 */
@Component
public class RoleMapper {

    /**
     * Map an entity into its response representation.
     *
     * @param role source entity
     * @return response DTO
     */
    public RoleResponse toResponse(Role role) {
        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .isSystemRole(role.isSystemRole())
                .build();
    }
}