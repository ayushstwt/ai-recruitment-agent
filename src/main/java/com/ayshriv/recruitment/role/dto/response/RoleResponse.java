package com.ayshriv.recruitment.role.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Role representation exposed to API clients.
 *
 * <p>Never exposes the JPA entity, the lazy user collection or any internal
 * persistence detail.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleResponse {

    private Long id;

    private String name;

    private String description;

    @JsonProperty("isSystemRole")
    private boolean isSystemRole;
}