package com.ayshriv.recruitment.apiKey.controller;

import com.ayshriv.recruitment.apiKey.dto.request.CreateApiKeyRequest;
import com.ayshriv.recruitment.apiKey.dto.request.UpdateApiKeyRequest;
import com.ayshriv.recruitment.apiKey.dto.response.ApiKeyCreatedResponse;
import com.ayshriv.recruitment.apiKey.dto.response.ApiKeyResponse;
import com.ayshriv.recruitment.apiKey.service.ApiKeyService;
import com.ayshriv.recruitment.common.response.ApiResponse;
import com.ayshriv.recruitment.common.response.ResponseUtil;
import com.ayshriv.recruitment.common.security.SecurityContextService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for managing API keys. All operations are scoped to the
 * organization of the authenticated caller.
 */
@RestController
@RequestMapping("/api/v1/api-keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final SecurityContextService securityContextService;

    /**
     * Create a new API key. The raw key is returned once in this response.
     *
     * @param request     creation payload
     * @param httpRequest servlet request for the response path
     * @return created key
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ApiKeyCreatedResponse>> create(
            @Valid @RequestBody CreateApiKeyRequest request, HttpServletRequest httpRequest) {
        Long organizationId = securityContextService.getCurrentOrganizationId();
        ApiKeyCreatedResponse created = apiKeyService.create(organizationId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.success("API key created successfully", created, httpRequest.getRequestURI()));
    }

    /**
     * List API keys of the current organization.
     *
     * @param httpRequest servlet request for the response path
     * @return list of keys
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ApiKeyResponse>>> list(HttpServletRequest httpRequest) {
        Long organizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "API keys retrieved successfully", apiKeyService.list(organizationId), httpRequest.getRequestURI()));
    }

    /**
     * Get a single API key.
     *
     * @param id          key primary key
     * @param httpRequest servlet request for the response path
     * @return key
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> get(
            @PathVariable Long id, HttpServletRequest httpRequest) {
        Long organizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "API key retrieved successfully", apiKeyService.get(organizationId, id), httpRequest.getRequestURI()));
    }

    /**
     * Update display attributes of an API key.
     *
     * @param id          key primary key
     * @param request     update payload
     * @param httpRequest servlet request for the response path
     * @return updated key
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> update(
            @PathVariable Long id, @Valid @RequestBody UpdateApiKeyRequest request, HttpServletRequest httpRequest) {
        Long organizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "API key updated successfully", apiKeyService.update(organizationId, id, request),
                httpRequest.getRequestURI()));
    }

    /**
     * Activate an API key.
     *
     * @param id          key primary key
     * @param httpRequest servlet request for the response path
     * @return updated key
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> activate(
            @PathVariable Long id, HttpServletRequest httpRequest) {
        Long organizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "API key activated successfully", apiKeyService.activate(organizationId, id),
                httpRequest.getRequestURI()));
    }

    /**
     * Deactivate an API key.
     *
     * @param id          key primary key
     * @param httpRequest servlet request for the response path
     * @return updated key
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<ApiKeyResponse>> deactivate(
            @PathVariable Long id, HttpServletRequest httpRequest) {
        Long organizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "API key deactivated successfully", apiKeyService.deactivate(organizationId, id),
                httpRequest.getRequestURI()));
    }

    /**
     * Soft delete an API key.
     *
     * @param id key primary key
     * @return empty {@code 204} response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Long organizationId = securityContextService.getCurrentOrganizationId();
        apiKeyService.delete(organizationId, id);
        return ResponseEntity.noContent().build();
    }
}