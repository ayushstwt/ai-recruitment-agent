package com.ayshriv.recruitment.role.controller;

import com.ayshriv.recruitment.common.response.ApiResponse;
import com.ayshriv.recruitment.common.response.ResponseUtil;
import com.ayshriv.recruitment.common.security.SecurityContextService;
import com.ayshriv.recruitment.role.dto.response.RoleResponse;
import com.ayshriv.recruitment.role.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for role discovery.
 *
 * <p>All operations are scoped to the authenticated tenant: an organization
 * only ever sees its own organization roles plus the globally shared system
 * roles. Role creation and modification are intentionally not exposed yet —
 * organization users must not be able to alter system roles.</p>
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@SecurityRequirement(name = "X-API-KEY")
@Tag(name = "Roles", description = "Role discovery for the authenticated organization")
public class RoleController {

    private final RoleService roleService;
    private final SecurityContextService securityContextService;

    /**
     * List roles accessible to the authenticated organization.
     *
     * @param httpRequest servlet request for the response path
     * @return accessible roles
     */
    @Operation(
            summary = "List accessible roles",
            description = "Returns the organization roles of the authenticated organization "
                    + "together with the globally shared system roles."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleResponse>>> list(HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "Roles retrieved successfully", roleService.getRoles(currentOrganizationId),
                httpRequest.getRequestURI()));
    }

    /**
     * List the globally shared system roles.
     *
     * @param httpRequest servlet request for the response path
     * @return system roles
     */
    @Operation(
            summary = "List system roles",
            description = "Returns the globally shared system roles such as AGENCY_ADMIN "
                    + "and RECRUITER."
    )
    @GetMapping("/system")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> listSystem(HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ResponseUtil.success(
                "System roles retrieved successfully", roleService.getSystemRoles(),
                httpRequest.getRequestURI()));
    }

    /**
     * Get a single role by id.
     *
     * @param id          role primary key
     * @param httpRequest servlet request for the response path
     * @return requested role
     */
    @Operation(
            summary = "Get role",
            description = "Returns a single role. Roles belonging to another "
                    + "organization are never returned."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleResponse>> get(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "Role retrieved successfully", roleService.getRoleById(currentOrganizationId, id),
                httpRequest.getRequestURI()));
    }
}