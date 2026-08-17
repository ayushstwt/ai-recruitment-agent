package com.ayshriv.recruitment.organization.controller;

import com.ayshriv.recruitment.common.response.ApiResponse;
import com.ayshriv.recruitment.common.response.ResponseUtil;
import com.ayshriv.recruitment.common.security.SecurityContextService;
import com.ayshriv.recruitment.organization.dto.request.CreateOrganizationRequest;
import com.ayshriv.recruitment.organization.dto.request.UpdateOrganizationRequest;
import com.ayshriv.recruitment.organization.dto.response.OrganizationResponse;
import com.ayshriv.recruitment.organization.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
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
 * REST endpoints for organization and multi-tenant management.
 *
 * <p>Except for the provisioning endpoint, every operation is scoped to the
 * authenticated tenant. The tenant id is resolved from the API key security
 * context and never accepted from the client; the service rejects any
 * request that targets another organization with {@code 403}.</p>
 */
@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@SecurityRequirement(name = "X-API-KEY")
@Tag(name = "Organizations", description = "Organization and multi-tenant management")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final SecurityContextService securityContextService;

    /**
     * Provision a new organization.
     *
     * @param request     creation payload
     * @param httpRequest servlet request for the response path
     * @return created organization
     */
    @Operation(
            summary = "Provision a new organization",
            description = "Controlled provisioning endpoint used to create a tenant. "
                    + "A brand new organization cannot authenticate with one of its own API keys yet, "
                    + "therefore this operation does not require an API key. It will be restricted to "
                    + "a platform admin authentication flow when one is introduced."
    )
    @SecurityRequirements
    @PostMapping
    @ApiResponses(@io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Organization created successfully",
            content = @Content(schema = @Schema(implementation = OrganizationResponse.class))))
    public ResponseEntity<ApiResponse<OrganizationResponse>> create(
            @Valid @RequestBody CreateOrganizationRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.success(
                        "Organization created successfully",
                        organizationService.createOrganization(request),
                        httpRequest.getRequestURI()));
    }

    /**
     * Page through non deleted organizations.
     *
     * @param pageable    pagination and sorting
     * @param httpRequest servlet request for the response path
     * @return paginated list of organizations
     */
    @Operation(
            summary = "List organizations",
            description = "Returns a paginated list of non deleted organizations."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrganizationResponse>>> list(
            @ParameterObject @PageableDefault(size = 20) Pageable pageable, HttpServletRequest httpRequest) {
        Page<OrganizationResponse> page = organizationService.getOrganizations(pageable);
        return ResponseEntity.ok(ResponseUtil.successPage(
                "Organizations retrieved successfully", page, httpRequest.getRequestURI()));
    }

    /**
     * Search organizations by keyword.
     *
     * @param keyword     search keyword across name, legal name and email
     * @param pageable    pagination and sorting
     * @param httpRequest servlet request for the response path
     * @return paginated list of matching organizations
     */
    @Operation(
            summary = "Search organizations",
            description = "Search non deleted organizations by keyword across name, "
                    + "legal name and email."
    )
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<OrganizationResponse>>> search(
            @RequestParam(required = false)
            @Parameter(description = "Keyword matched against name, legal name and email")
            String keyword,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdOn", direction = Sort.Direction.DESC)
            Pageable pageable,
            HttpServletRequest httpRequest) {
        Page<OrganizationResponse> page = organizationService.searchOrganizations(keyword, pageable);
        return ResponseEntity.ok(ResponseUtil.successPage(
                "Organizations retrieved successfully", page, httpRequest.getRequestURI()));
    }

    /**
     * Get an organization by id.
     *
     * @param id          organization primary key
     * @param httpRequest servlet request for the response path
     * @return requested organization
     */
    @Operation(
            summary = "Get organization",
            description = "Returns a single non deleted organization. Only the authenticated "
                    + "organization can be retrieved."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> get(
            @PathVariable Long id, HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "Organization retrieved successfully",
                organizationService.getOrganizationById(currentOrganizationId, id),
                httpRequest.getRequestURI()));
    }

    /**
     * Update an organization profile.
     *
     * @param id          organization primary key
     * @param request     update payload
     * @param httpRequest servlet request for the response path
     * @return updated organization
     */
    @Operation(
            summary = "Update organization",
            description = "Updates the profile of the authenticated organization. "
                    + "Lifecycle fields and the active flag cannot be modified through this endpoint."
    )
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OrganizationResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrganizationRequest request,
            HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "Organization updated successfully",
                organizationService.updateOrganization(currentOrganizationId, id, request),
                httpRequest.getRequestURI()));
    }

    /**
     * Activate an organization.
     *
     * @param id          organization primary key
     * @param httpRequest servlet request for the response path
     * @return activated organization
     */
    @Operation(
            summary = "Activate organization",
            description = "Activates the authenticated organization."
    )
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<OrganizationResponse>> activate(
            @PathVariable Long id, HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "Organization activated successfully",
                organizationService.activateOrganization(currentOrganizationId, id),
                httpRequest.getRequestURI()));
    }

    /**
     * Deactivate an organization.
     *
     * @param id          organization primary key
     * @param httpRequest servlet request for the response path
     * @return deactivated organization
     */
    @Operation(
            summary = "Deactivate organization",
            description = "Deactivates the authenticated organization without deleting it."
    )
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<OrganizationResponse>> deactivate(
            @PathVariable Long id, HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "Organization deactivated successfully",
                organizationService.deactivateOrganization(currentOrganizationId, id),
                httpRequest.getRequestURI()));
    }

    /**
     * Soft delete an organization.
     *
     * @param id organization primary key
     * @return empty response
     */
    @Operation(
            summary = "Delete organization",
            description = "Soft deletes the authenticated organization. The record is not "
                    + "physically removed."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        organizationService.deleteOrganization(currentOrganizationId, id);
        return ResponseEntity.noContent().build();
    }
}
