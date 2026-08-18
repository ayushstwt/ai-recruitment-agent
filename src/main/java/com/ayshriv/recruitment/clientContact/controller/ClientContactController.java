package com.ayshriv.recruitment.clientContact.controller;

import com.ayshriv.recruitment.clientContact.dto.request.CreateClientContactRequest;
import com.ayshriv.recruitment.clientContact.dto.request.UpdateClientContactRequest;
import com.ayshriv.recruitment.clientContact.dto.response.ClientContactResponse;
import com.ayshriv.recruitment.clientContact.service.ClientContactService;
import com.ayshriv.recruitment.common.response.ApiResponse;
import com.ayshriv.recruitment.common.response.ResponseUtil;
import com.ayshriv.recruitment.common.security.SecurityContextService;
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
 * REST endpoints for client contact management.
 *
 * <p>Contacts are nested under a client ({@code /clients/{clientId}/contacts}).
 * Every operation verifies that both the owning client and the contact belong
 * to the authenticated organization; the tenant is resolved from the API key
 * security context and never accepted from the client.</p>
 */
@RestController
@RequestMapping("/api/v1/clients/{clientId}/contacts")
@RequiredArgsConstructor
@SecurityRequirement(name = "X-API-KEY")
@Tag(name = "Client Contacts", description = "Contacts inside a client / hiring company")
public class ClientContactController {

    private final ClientContactService contactService;
    private final SecurityContextService securityContextService;

    /**
     * Create a contact for a client.
     *
     * @param clientId    owning client id
     * @param request     creation payload
     * @param httpRequest servlet request for the response path
     * @return created contact
     */
    @Operation(
            summary = "Create client contact",
            description = "Creates a contact inside a client company of the authenticated "
                    + "organization. The client is addressed by the URL and the organization is "
                    + "derived from the API key; neither may be supplied in the payload."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<ClientContactResponse>> create(
            @PathVariable Long clientId,
            @Valid @RequestBody CreateClientContactRequest request,
            HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.success(
                        "Client contact created successfully",
                        contactService.createContact(currentOrganizationId, clientId, request),
                        httpRequest.getRequestURI()));
    }

    /**
     * Page through all non deleted contacts of a client.
     *
     * @param clientId    owning client id
     * @param pageable    pagination and sorting
     * @param httpRequest servlet request for the response path
     * @return paginated list of contacts
     */
    @Operation(
            summary = "List client contacts",
            description = "Returns a paginated list of non deleted contacts of a client."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<ClientContactResponse>>> list(
            @PathVariable Long clientId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdOn", direction = Sort.Direction.DESC)
            Pageable pageable,
            HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        Page<ClientContactResponse> page = contactService.getContacts(currentOrganizationId, clientId, pageable);
        return ResponseEntity.ok(ResponseUtil.successPage(
                "Client contacts retrieved successfully", page, httpRequest.getRequestURI()));
    }

    /**
     * Search contacts of a client by keyword.
     *
     * @param clientId    owning client id
     * @param keyword     search keyword across first name, last name, email, job title and department
     * @param pageable    pagination and sorting
     * @param httpRequest servlet request for the response path
     * @return paginated list of matching contacts
     */
    @Operation(
            summary = "Search client contacts",
            description = "Search non deleted contacts of a client by keyword across first "
                    + "name, last name, email, job title and department."
    )
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ClientContactResponse>>> search(
            @PathVariable Long clientId,
            @RequestParam(required = false)
            @Parameter(description = "Keyword matched against first name, last name, email, job title and department")
            String keyword,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdOn", direction = Sort.Direction.DESC)
            Pageable pageable,
            HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        Page<ClientContactResponse> page =
                contactService.searchContacts(currentOrganizationId, clientId, keyword, pageable);
        return ResponseEntity.ok(ResponseUtil.successPage(
                "Client contacts retrieved successfully", page, httpRequest.getRequestURI()));
    }

    /**
     * Get a single contact of a client.
     *
     * @param clientId    owning client id
     * @param contactId   contact primary key
     * @param httpRequest servlet request for the response path
     * @return requested contact
     */
    @Operation(
            summary = "Get client contact",
            description = "Returns a single non deleted contact of a client. Contacts of "
                    + "another organization or another client are rejected with 403."
    )
    @GetMapping("/{contactId}")
    public ResponseEntity<ApiResponse<ClientContactResponse>> get(
            @PathVariable Long clientId,
            @PathVariable Long contactId,
            HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "Client contact retrieved successfully",
                contactService.getContact(currentOrganizationId, clientId, contactId),
                httpRequest.getRequestURI()));
    }

    /**
     * Update a contact profile.
     *
     * @param clientId    owning client id
     * @param contactId   contact primary key
     * @param request     update payload
     * @param httpRequest servlet request for the response path
     * @return updated contact
     */
    @Operation(
            summary = "Update client contact",
            description = "Updates profile fields of a contact. The client, the tenant, "
                    + "lifecycle timestamps and the activation state cannot be modified through "
                    + "this endpoint; activation is changed through the dedicated endpoints."
    )
    @PutMapping("/{contactId}")
    public ResponseEntity<ApiResponse<ClientContactResponse>> update(
            @PathVariable Long clientId,
            @PathVariable Long contactId,
            @Valid @RequestBody UpdateClientContactRequest request,
            HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "Client contact updated successfully",
                contactService.updateContact(currentOrganizationId, clientId, contactId, request),
                httpRequest.getRequestURI()));
    }

    /**
     * Activate a contact.
     *
     * @param clientId    owning client id
     * @param contactId   contact primary key
     * @param httpRequest servlet request for the response path
     * @return activated contact
     */
    @Operation(
            summary = "Activate client contact",
            description = "Sets the active flag of a contact to true without changing the "
                    + "deleted flag."
    )
    @PatchMapping("/{contactId}/activate")
    public ResponseEntity<ApiResponse<ClientContactResponse>> activate(
            @PathVariable Long clientId,
            @PathVariable Long contactId,
            HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "Client contact activated successfully",
                contactService.activateContact(currentOrganizationId, clientId, contactId),
                httpRequest.getRequestURI()));
    }

    /**
     * Deactivate a contact.
     *
     * @param clientId    owning client id
     * @param contactId   contact primary key
     * @param httpRequest servlet request for the response path
     * @return deactivated contact
     */
    @Operation(
            summary = "Deactivate client contact",
            description = "Sets the active flag of a contact to false without deleting the contact."
    )
    @PatchMapping("/{contactId}/deactivate")
    public ResponseEntity<ApiResponse<ClientContactResponse>> deactivate(
            @PathVariable Long clientId,
            @PathVariable Long contactId,
            HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "Client contact deactivated successfully",
                contactService.deactivateContact(currentOrganizationId, clientId, contactId),
                httpRequest.getRequestURI()));
    }

    /**
     * Soft delete a contact.
     *
     * @param clientId  owning client id
     * @param contactId contact primary key
     * @return empty response
     */
    @Operation(
            summary = "Delete client contact",
            description = "Soft deletes a contact: the active flag is set to false and the "
                    + "deleted flag to true. The record is not physically removed."
    )
    @DeleteMapping("/{contactId}")
    public ResponseEntity<Void> delete(@PathVariable Long clientId, @PathVariable Long contactId) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        contactService.deleteContact(currentOrganizationId, clientId, contactId);
        return ResponseEntity.noContent().build();
    }
}