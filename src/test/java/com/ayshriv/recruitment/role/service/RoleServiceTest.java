package com.ayshriv.recruitment.role.service;

import com.ayshriv.recruitment.common.exception.ForbiddenException;
import com.ayshriv.recruitment.role.dto.response.RoleResponse;
import com.ayshriv.recruitment.role.entity.Role;
import com.ayshriv.recruitment.role.exception.RoleNotFoundException;
import com.ayshriv.recruitment.role.mapper.RoleMapper;
import com.ayshriv.recruitment.role.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private RoleRepository roleRepository;

    @Spy
    private RoleMapper roleMapper = new RoleMapper();

    @InjectMocks
    private RoleService roleService;

    private Role systemRole(Long id, String name) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        role.setSystemRole(true);
        role.setActive(true);
        role.setDeleted(false);
        return role;
    }

    private Role orgRole(Long id, String name, Long organizationId) {
        Role role = systemRole(id, name);
        role.setSystemRole(false);
        com.ayshriv.recruitment.organization.entity.Organization organization =
                new com.ayshriv.recruitment.organization.entity.Organization();
        organization.setId(organizationId);
        role.setOrganization(organization);
        return role;
    }

    @Test
    void getRolesReturnsOrganizationRolesAndSystemRoles() {
        when(roleRepository.findAccessibleRoles(10L))
                .thenReturn(List.of(systemRole(1L, "VIEWER"), orgRole(9L, "CUSTOM", 10L)));

        List<RoleResponse> roles = roleService.getRoles(10L);

        assertThat(roles).hasSize(2);
        assertThat(roles.get(0).getName()).isEqualTo("VIEWER");
        verify(roleRepository).findAccessibleRoles(10L);
    }

    @Test
    void getSystemRolesReturnsOnlySystemRoles() {
        when(roleRepository.findSystemRoles()).thenReturn(List.of(systemRole(1L, "RECRUITER")));

        List<RoleResponse> roles = roleService.getSystemRoles();

        assertThat(roles).hasSize(1);
        assertThat(roles.get(0).getName()).isEqualTo("RECRUITER");
        assertThat(roles.get(0).isSystemRole()).isTrue();
    }

    @Test
    void getRoleByIdReturnsAccessibleRole() {
        when(roleRepository.findAccessibleRole(3L, 10L)).thenReturn(Optional.of(systemRole(3L, "RECRUITER")));

        RoleResponse response = roleService.getRoleById(10L, 3L);

        assertThat(response.getId()).isEqualTo(3L);
        assertThat(response.getName()).isEqualTo("RECRUITER");
    }

    @Test
    void getRoleByIdThrowsNotFoundWhenRoleNotAccessible() {
        when(roleRepository.findAccessibleRole(99L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.getRoleById(10L, 99L))
                .isInstanceOf(RoleNotFoundException.class)
                .extracting(ex -> ((RoleNotFoundException) ex).getCode())
                .isEqualTo("ROLE_NOT_FOUND");
    }

    @Test
    void getRoleForAssignmentAllowsSystemRoleToAnyOrganization() {
        when(roleRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(systemRole(1L, "VIEWER")));

        Role role = roleService.getRoleForAssignment(1L, 10L);

        assertThat(role.getName()).isEqualTo("VIEWER");
    }

    @Test
    void getRoleForAssignmentAllowsOwnOrganizationRole() {
        when(roleRepository.findByIdAndNotDeleted(9L)).thenReturn(Optional.of(orgRole(9L, "CUSTOM", 10L)));

        Role role = roleService.getRoleForAssignment(9L, 10L);

        assertThat(role.getName()).isEqualTo("CUSTOM");
    }

    @Test
    void getRoleForAssignmentRejectsRoleOfAnotherOrganization() {
        when(roleRepository.findByIdAndNotDeleted(9L)).thenReturn(Optional.of(orgRole(9L, "CUSTOM", 20L)));

        assertThatThrownBy(() -> roleService.getRoleForAssignment(9L, 10L))
                .isInstanceOf(ForbiddenException.class)
                .extracting(ex -> ((ForbiddenException) ex).getCode())
                .isEqualTo("ROLE_ACCESS_DENIED");
    }

    @Test
    void getRoleForAssignmentThrowsNotFoundWhenRoleMissing() {
        when(roleRepository.findByIdAndNotDeleted(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.getRoleForAssignment(99L, 10L))
                .isInstanceOf(RoleNotFoundException.class)
                .extracting(ex -> ((RoleNotFoundException) ex).getCode())
                .isEqualTo("ROLE_NOT_FOUND");
    }

    @Test
    void toResponsesOrdersRolesByName() {
        List<RoleResponse> responses = roleService.toResponses(
                List.of(systemRole(1L, "RECRUITER"), systemRole(2L, "AGENCY_ADMIN")));

        assertThat(responses.stream().map(RoleResponse::getName).toList())
                .containsExactly("AGENCY_ADMIN", "RECRUITER");
    }
}