package com.ayshriv.recruitment.user.service;

import com.ayshriv.recruitment.common.exception.BadRequestException;
import com.ayshriv.recruitment.common.exception.ForbiddenException;
import com.ayshriv.recruitment.common.security.PasswordPolicyValidator;
import com.ayshriv.recruitment.organization.entity.Organization;
import com.ayshriv.recruitment.role.dto.response.RoleResponse;
import com.ayshriv.recruitment.role.entity.Role;
import com.ayshriv.recruitment.role.service.RoleService;
import com.ayshriv.recruitment.user.dto.request.AssignRoleRequest;
import com.ayshriv.recruitment.user.dto.request.ChangePasswordRequest;
import com.ayshriv.recruitment.user.dto.request.CreateUserRequest;
import com.ayshriv.recruitment.user.dto.request.UpdateUserRequest;
import com.ayshriv.recruitment.user.dto.response.UserResponse;
import com.ayshriv.recruitment.user.dto.response.UserSummaryResponse;
import com.ayshriv.recruitment.user.entity.User;
import com.ayshriv.recruitment.user.exception.DuplicateUserException;
import com.ayshriv.recruitment.user.exception.UserNotFoundException;
import com.ayshriv.recruitment.user.mapper.UserMapper;
import com.ayshriv.recruitment.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Business logic for user management.
 *
 * <p>Every operation is tenant-bound: the organization id is always resolved
 * from the security context (never from the client) and every lookup verifies
 * that the target user belongs to the authenticated organization. A user of
 * another organization surfaces as {@code 403 FORBIDDEN}, a missing or
 * soft-deleted user as {@code 404 NOT FOUND}.</p>
 *
 * <p>Passwords are validated against the configured policy, hashed with the
 * {@link PasswordEncoder} and only ever stored as {@code passwordHash}. The
 * hash is never returned or logged.</p>
 */
@Service
@RequiredArgsConstructor
public class UserService {

    /**
     * Machine readable code returned when a user of another organization is
     * accessed.
     */
    public static final String USER_ACCESS_DENIED = "USER_ACCESS_DENIED";

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;

    /**
     * Create a user within the authenticated organization.
     *
     * @param currentOrganizationId authenticated tenant id
     * @param request               creation payload
     * @return created user
     * @throws DuplicateUserException when the email is already used in the organization
     */
    @Transactional
    public UserResponse createUser(Long currentOrganizationId, CreateUserRequest request) {
        passwordPolicyValidator.validate(request.getPassword());
        ensureEmailAvailable(currentOrganizationId, request.getEmail(), null);

        User user = userMapper.toEntity(request);
        user.setOrganization(new Organization(currentOrganizationId));
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.getRoles().addAll(loadAccessibleRoles(currentOrganizationId, request.getRoleIds()));

        return userMapper.toResponse(userRepository.save(user));
    }

    /**
     * Get a single user by id.
     *
     * @param currentOrganizationId authenticated tenant id
     * @param userId                requested user id
     * @return requested user
     * @throws UserNotFoundException when the user is missing or soft deleted
     * @throws ForbiddenException    when the user belongs to another organization
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long currentOrganizationId, Long userId) {
        return userMapper.toResponse(findUserInOrganization(currentOrganizationId, userId));
    }

    /**
     * Page through all non deleted users of the authenticated organization.
     *
     * @param currentOrganizationId authenticated tenant id
     * @param pageable              pagination and sorting
     * @return page of users
     */
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> getUsers(Long currentOrganizationId, Pageable pageable) {
        return userRepository.findAllByOrganization(currentOrganizationId, pageable)
                .map(userMapper::toSummaryResponse);
    }

    /**
     * Search non deleted users of the authenticated organization.
     *
     * @param currentOrganizationId authenticated tenant id
     * @param keyword               search keyword; blank returns all users
     * @param pageable              pagination and sorting
     * @return page of matching users
     */
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> searchUsers(Long currentOrganizationId, String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return getUsers(currentOrganizationId, pageable);
        }
        return userRepository.searchUsers(currentOrganizationId, keyword.trim(), pageable)
                .map(userMapper::toSummaryResponse);
    }

    /**
     * Update the profile of a user.
     *
     * @param currentOrganizationId authenticated tenant id
     * @param userId                requested user id
     * @param request               update payload
     * @return updated user
     */
    @Transactional
    public UserResponse updateUser(Long currentOrganizationId, Long userId, UpdateUserRequest request) {
        User user = findUserInOrganization(currentOrganizationId, userId);
        if (!request.getEmail().equalsIgnoreCase(user.getEmail())) {
            ensureEmailAvailable(currentOrganizationId, request.getEmail(), userId);
        }
        userMapper.updateEntity(user, request);
        return userMapper.toResponse(userRepository.save(user));
    }

    /**
     * Activate a user without touching the deleted flag.
     *
     * @param currentOrganizationId authenticated tenant id
     * @param userId                requested user id
     * @return activated user
     */
    @Transactional
    public UserResponse activateUser(Long currentOrganizationId, Long userId) {
        User user = findUserInOrganization(currentOrganizationId, userId);
        user.setActive(true);
        return userMapper.toResponse(userRepository.save(user));
    }

    /**
     * Deactivate a user without deleting it.
     *
     * @param currentOrganizationId authenticated tenant id
     * @param userId                requested user id
     * @return deactivated user
     */
    @Transactional
    public UserResponse deactivateUser(Long currentOrganizationId, Long userId) {
        User user = findUserInOrganization(currentOrganizationId, userId);
        user.setActive(false);
        return userMapper.toResponse(userRepository.save(user));
    }

    /**
     * Soft delete a user. The record is never physically removed.
     *
     * @param currentOrganizationId authenticated tenant id
     * @param userId                requested user id
     */
    @Transactional
    public void deleteUser(Long currentOrganizationId, Long userId) {
        User user = findUserInOrganization(currentOrganizationId, userId);
        user.softDelete();
        userRepository.save(user);
    }

    /**
     * Change a user's password after verifying the current one.
     *
     * @param currentOrganizationId authenticated tenant id
     * @param userId                requested user id
     * @param request               password payload
     * @throws BadRequestException when the current password is wrong or the
     *                             new password violates the policy
     */
    @Transactional
    public void changePassword(Long currentOrganizationId, Long userId, ChangePasswordRequest request) {
        User user = findUserInOrganization(currentOrganizationId, userId);
        passwordPolicyValidator.validate(request.getNewPassword());
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect", PasswordPolicyValidator.INVALID_PASSWORD);
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * Assign roles to a user. Every role must belong to the same organization
     * or be a shared system role; duplicates are ignored.
     *
     * @param currentOrganizationId authenticated tenant id
     * @param userId                requested user id
     * @param request               role payload
     * @return updated user
     */
    @Transactional
    public UserResponse assignRoles(Long currentOrganizationId, Long userId, AssignRoleRequest request) {
        User user = findUserInOrganization(currentOrganizationId, userId);
        Set<Role> accessibleRoles = loadAccessibleRoles(currentOrganizationId, request.getRoleIds());
        user.getRoles().addAll(accessibleRoles);
        return userMapper.toResponse(userRepository.save(user));
    }

    /**
     * Remove a single role from a user.
     *
     * @param currentOrganizationId authenticated tenant id
     * @param userId                requested user id
     * @param roleId                role to remove
     */
    @Transactional
    public void removeRole(Long currentOrganizationId, Long userId, Long roleId) {
        User user = findUserInOrganization(currentOrganizationId, userId);
        Role role = roleService.getRoleForAssignment(roleId, currentOrganizationId);
        user.getRoles().remove(role);
        userRepository.save(user);
    }

    /**
     * List the roles of a user.
     *
     * @param currentOrganizationId authenticated tenant id
     * @param userId                requested user id
     * @return roles of the user
     */
    @Transactional(readOnly = true)
    public List<RoleResponse> getUserRoles(Long currentOrganizationId, Long userId) {
        User user = findUserInOrganization(currentOrganizationId, userId);
        return roleService.toResponses(user.getRoles());
    }

    /**
     * Load a non deleted user and verify it belongs to the authenticated
     * organization. A user of another organization is forbidden, a missing or
     * soft-deleted user is not found.
     *
     * @param currentOrganizationId authenticated tenant id
     * @param userId                requested user id
     * @return matching user
     */
    private User findUserInOrganization(Long currentOrganizationId, Long userId) {
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (!user.getOrganizationId().equals(currentOrganizationId)) {
            throw new ForbiddenException("Access denied to the requested user", USER_ACCESS_DENIED);
        }
        return user;
    }

    /**
     * Ensure the email is not already used by another non deleted user of the
     * same organization. Soft deleted users do not block reuse.
     *
     * @param organizationId owning tenant
     * @param email          candidate email
     * @param excludingId    user id to exclude (own id on update), or {@code null}
     */
    private void ensureEmailAvailable(Long organizationId, String email, Long excludingId) {
        userRepository.findByEmailAndOrganization(email, organizationId).ifPresent(existing -> {
            if (excludingId == null || !existing.getId().equals(excludingId)) {
                throw new DuplicateUserException(email);
            }
        });
    }

    /**
     * Resolve requested role ids into assignable roles. Every role must belong
     * to the same organization or be a shared system role.
     *
     * @param currentOrganizationId authenticated tenant id
     * @param roleIds               requested role ids, may be {@code null}
     * @return assignable roles without duplicates
     */
    private Set<Role> loadAccessibleRoles(Long currentOrganizationId, List<Long> roleIds) {
        Set<Role> roles = new LinkedHashSet<>();
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                roles.add(roleService.getRoleForAssignment(roleId, currentOrganizationId));
            }
        }
        return roles;
    }
}