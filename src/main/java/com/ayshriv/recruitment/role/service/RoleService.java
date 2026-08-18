package com.ayshriv.recruitment.role.service;

import com.ayshriv.recruitment.common.exception.ForbiddenException;
import com.ayshriv.recruitment.role.dto.response.RoleResponse;
import com.ayshriv.recruitment.role.entity.Role;
import com.ayshriv.recruitment.role.exception.RoleNotFoundException;
import com.ayshriv.recruitment.role.mapper.RoleMapper;
import com.ayshriv.recruitment.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Business logic for role management.
 *
 * <p>Roles are tenant-aware: an organization can only see and use its own
 * organization roles plus the globally shared system roles. No mutation
 * operations are exposed yet; the module currently only serves role
 * discovery and the accessibility checks required for user assignment.</p>
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    /**
     * Machine readable code returned when a requested role belongs to another
     * organization and is not a shared system role.
     */
    public static final String ROLE_ACCESS_DENIED = "ROLE_ACCESS_DENIED";

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;

    /**
     * List every role the organization is allowed to use: its own organization
     * roles plus all system roles.
     *
     * @param currentOrganizationId authenticated tenant id
     * @return accessible roles
     */
    @Transactional(readOnly = true)
    public List<RoleResponse> getRoles(Long currentOrganizationId) {
        return roleRepository.findAccessibleRoles(currentOrganizationId)
                .stream()
                .map(roleMapper::toResponse)
                .toList();
    }

    /**
     * List the globally shared system roles.
     *
     * @return system roles
     */
    @Transactional(readOnly = true)
    public List<RoleResponse> getSystemRoles() {
        return roleRepository.findSystemRoles()
                .stream()
                .map(roleMapper::toResponse)
                .toList();
    }

    /**
     * Get a single role by id, scoped to roles the organization is allowed to
     * use. Roles belonging to another organization resolve to not found.
     *
     * @param currentOrganizationId authenticated tenant id
     * @param roleId                requested role id
     * @return requested role
     * @throws RoleNotFoundException when the role is missing or not accessible
     */
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Long currentOrganizationId, Long roleId) {
        return roleMapper.toResponse(getAccessibleRole(roleId, currentOrganizationId));
    }

    /**
     * Load a role the organization is allowed to use.
     *
     * @param roleId                requested role id
     * @param currentOrganizationId authenticated tenant id
     * @return accessible role
     * @throws RoleNotFoundException when the role is missing or not accessible
     */
    @Transactional(readOnly = true)
    public Role getAccessibleRole(Long roleId, Long currentOrganizationId) {
        return roleRepository.findAccessibleRole(roleId, currentOrganizationId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));
    }

    /**
     * Load a role for assignment or removal, distinguishing a missing role
     * from a role that exists but belongs to another organization.
     *
     * <p>A user may only be given roles of its own organization or shared
     * system roles.</p>
     *
     * @param roleId                requested role id
     * @param currentOrganizationId authenticated tenant id
     * @return assignable role
     * @throws RoleNotFoundException when the role does not exist
     * @throws ForbiddenException    when the role belongs to another organization
     */
    @Transactional(readOnly = true)
    public Role getRoleForAssignment(Long roleId, Long currentOrganizationId) {
        Role role = roleRepository.findByIdAndNotDeleted(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));
        if (!role.isSystemRole() && !role.getOrganizationId().equals(currentOrganizationId)) {
            throw new ForbiddenException("Access denied to the requested role", ROLE_ACCESS_DENIED);
        }
        return role;
    }

    /**
     * Map a collection of roles into response DTOs, ordered by name.
     *
     * @param roles source roles
     * @return ordered response DTOs
     */
    public List<RoleResponse> toResponses(Collection<Role> roles) {
        return roles.stream()
                .sorted(Comparator.comparing(Role::getName))
                .map(roleMapper::toResponse)
                .toList();
    }
}