package com.ayshriv.recruitment.role.mapper;

import com.ayshriv.recruitment.role.dto.response.RoleResponse;
import com.ayshriv.recruitment.role.entity.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleMapperTest {

    private final RoleMapper mapper = new RoleMapper();

    @Test
    void toResponseMapsEveryField() {
        Role role = new Role();
        role.setId(3L);
        role.setName("RECRUITER");
        role.setDescription("Manage candidates, jobs and applications assigned to them.");
        role.setSystemRole(true);

        RoleResponse response = mapper.toResponse(role);

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getName()).isEqualTo("RECRUITER");
        assertThat(response.getDescription())
                .isEqualTo("Manage candidates, jobs and applications assigned to them.");
        assertThat(response.isSystemRole()).isTrue();
    }
}