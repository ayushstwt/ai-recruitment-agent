package com.ayshriv.recruitment.user.controller;

import com.ayshriv.recruitment.common.response.ApiResponse;
import com.ayshriv.recruitment.common.response.ResponseUtil;
import com.ayshriv.recruitment.common.security.SecurityContextService;
import com.ayshriv.recruitment.role.dto.response.RoleResponse;
import com.ayshriv.recruitment.user.dto.request.AssignRoleRequest;
import com.ayshriv.recruitment.user.dto.request.ChangePasswordRequest;
import com.ayshriv.recruitment.user.dto.request.CreateUserRequest;
import com.ayshriv.recruitment.user.dto.request.UpdateUserRequest;
import com.ayshriv.recruitment.user.dto.response.UserResponse;
import com.ayshriv.recruitment.user.dto.response.UserSummaryResponse;
import com.ayshriv.recruitment.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for user and role management.
 *
 * <p>Every operation is scoped to the authenticated tenant. The organization
 * is resolved from the API key security context and never accepted from the
 * client; the service rejects any request that targets another organization's
 * user with {@code 403}.</p>
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "X-API-KEY")
@Tag(name = "Users", description = "User management and role assignment")
public class UserController {

    private final UserService userService;
    private final SecurityContextService securityContextService;

    /**
     * Create a user within the authenticated organization.
     *
     * @param request     creation payload
     * @param httpRequest servlet request for the response path
     * @return created user
     */
    @Operation(
            summary = "Create user",
            description = "Creates a user in the authenticated organization. The password is "
                    + "validated against the configured policy, hashed and never returned. "
                    + "The organization is derived from the API key, never from the payload."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(
            @Valid @RequestBody CreateUserRequest request, HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.success(
                        "User created successfully",
                        userService.createUser(currentOrganizationId, request),
                        httpRequest.getRequestURI()));
    }

    /**
     * Page through all non deleted users of the authenticated organization.
     *
     * @param pageable    pagination and sorting
     * @param httpRequest servlet request for the response path
     * @return paginated list of users
     */
    @Operation(
            summary = "List users",
            description = "Returns a paginated list of non deleted users of the "
                    + "authenticated organization."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> list(
            @ParameterObject @PageableDefault(size = 20, sort = "createdOn", direction = Sort.Direction.DESC)
            Pageable pageable, HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        Page<UserSummaryResponse> page = userService.getUsers(currentOrganizationId, pageable);
        return ResponseEntity.ok(ResponseUtil.successPage(
                "Users retrieved successfully", page, httpRequest.getRequestURI()));
    }

    /**
     * Search users by keyword.
     *
     * @param keyword     search keyword across first name, last name, email and job title
     * @param pageable    pagination and sorting
     * @param httpRequest servlet request for the response path
     * @return paginated list of matching users
     */
    @Operation(
            summary = "Search users",
            description = "Search non deleted users of the authenticated organization by keyword "
                    + "across first name, last name, email and job title."
    )
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserSummaryResponse>>> search(
            @RequestParam(required = false)
            @Parameter(description = "Keyword matched against first name, last name, email and job title")
            String keyword,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdOn", direction = Sort.Direction.DESC)
            Pageable pageable,
            HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        Page<UserSummaryResponse> page = userService.searchUsers(currentOrganizationId, keyword, pageable);
        return ResponseEntity.ok(ResponseUtil.successPage(
                "Users retrieved successfully", page, httpRequest.getRequestURI()));
    }

    /**
     * Get a single user by id.
     *
     * @param id          user primary key
     * @param httpRequest servlet request for the response path
     * @return requested user
     */
    @Operation(
            summary = "Get user",
            description = "Returns a single non deleted user. Users of another "
                    + "organization are rejected with 403."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> get(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "User retrieved successfully",
                userService.getUserById(currentOrganizationId, id),
                httpRequest.getRequestURI()));
    }

    /**
     * Update a user profile.
     *
     * @param id          user primary key
     * @param request     update payload
     * @param httpRequest servlet request for the response path
     * @return updated user
     */
    @Operation(
            summary = "Update user",
            description = "Updates profile fields of a user. Passwords, roles and activation "
                    + "state are changed through their dedicated endpoints."
    )
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "User updated successfully",
                userService.updateUser(currentOrganizationId, id, request),
                httpRequest.getRequestURI()));
    }

    /**
     * Activate a user.
     *
     * @param id          user primary key
     * @param httpRequest servlet request for the response path
     * @return activated user
     */
    @Operation(
            summary = "Activate user",
            description = "Sets the active flag of a user to true without changing the "
                    + "deleted flag."
    )
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<UserResponse>> activate(
            @PathVariable Long id, HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "User activated successfully",
                userService.activateUser(currentOrganizationId, id),
                httpRequest.getRequestURI()));
    }

    /**
     * Deactivate a user.
     *
     * @param id          user primary key
     * @param httpRequest servlet request for the response path
     * @return deactivated user
     */
    @Operation(
            summary = "Deactivate user",
            description = "Sets the active flag of a user to false without deleting the user."
    )
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<UserResponse>> deactivate(
            @PathVariable Long id, HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "User deactivated successfully",
                userService.deactivateUser(currentOrganizationId, id),
                httpRequest.getRequestURI()));
    }

    /**
     * Soft delete a user.
     *
     * @param id user primary key
     * @return empty response
     */
    @Operation(
            summary = "Delete user",
            description = "Soft deletes a user: the active flag is set to false and the "
                    + "deleted flag to true. The record is not physically removed."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        userService.deleteUser(currentOrganizationId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Change a user's password.
     *
     * @param id          user primary key
     * @param request     password payload
     * @param httpRequest servlet request for the response path
     * @return success response without any password data
     */
    @Operation(
            summary = "Change password",
            description = "Verifies the current password, validates the new one against the "
                    + "configured policy and stores only the hash. Password values are never "
                    + "returned."
    )
    @PatchMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        userService.changePassword(currentOrganizationId, id, request);
        return ResponseEntity.ok(ResponseUtil.success(
                "Password changed successfully", null, httpRequest.getRequestURI()));
    }

    /**
     * Assign roles to a user.
     *
     * @param id          user primary key
     * @param request     role payload
     * @param httpRequest servlet request for the response path
     * @return updated user
     */
    @Operation(
            summary = "Assign roles",
            description = "Assigns roles to a user. Roles must belong to the same organization "
                    + "or be shared system roles; duplicates are ignored."
    )
    @PostMapping("/{id}/roles")
    public ResponseEntity<ApiResponse<UserResponse>> assignRoles(
            @PathVariable Long id,
            @Valid @RequestBody AssignRoleRequest request,
            HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "Roles assigned successfully",
                userService.assignRoles(currentOrganizationId, id, request),
                httpRequest.getRequestURI()));
    }

    /**
     * List the roles of a user.
     *
     * @param id          user primary key
     * @param httpRequest servlet request for the response path
     * @return roles of the user
     */
    @Operation(
            summary = "Get user roles",
            description = "Returns the roles assigned to a user."
    )
    @GetMapping("/{id}/roles")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getRoles(
            @PathVariable Long id, HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "Roles retrieved successfully",
                userService.getUserRoles(currentOrganizationId, id),
                httpRequest.getRequestURI()));
    }

    /**
     * Remove a role from a user.
     *
     * @param id          user primary key
     * @param roleId      role primary key
     * @return empty response
     */
    @Operation(
            summary = "Remove role",
            description = "Removes a single role from a user. The role must belong to the "
                    + "same organization or be a shared system role."
    )
    @DeleteMapping("/{id}/roles/{roleId}")
    public ResponseEntity<Void> removeRole(@PathVariable Long id, @PathVariable Long roleId) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        userService.removeRole(currentOrganizationId, id, roleId);
        return ResponseEntity.noContent().build();
    }
}