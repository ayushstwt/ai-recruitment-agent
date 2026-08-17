package com.ayshriv.recruitment.organization.service;

import com.ayshriv.recruitment.common.exception.BadRequestException;
import com.ayshriv.recruitment.common.exception.DuplicateResourceException;
import com.ayshriv.recruitment.common.exception.ForbiddenException;
import com.ayshriv.recruitment.organization.dto.request.CreateOrganizationRequest;
import com.ayshriv.recruitment.organization.dto.request.UpdateOrganizationRequest;
import com.ayshriv.recruitment.organization.dto.response.OrganizationResponse;
import com.ayshriv.recruitment.organization.entity.Organization;
import com.ayshriv.recruitment.organization.exception.OrganizationNotFoundException;
import com.ayshriv.recruitment.organization.mapper.OrganizationMapper;
import com.ayshriv.recruitment.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;

/**
 * Business logic for organization management.
 *
 * <p>An organization is the tenant itself. Every operation that targets an
 * existing organization by id verifies that the requested organization is the
 * same one the authenticated caller belongs to. The authenticated tenant id
 * is always resolved from the security context (an API key principal), never
 * from client provided input.</p>
 *
 * <p>Organization creation is a provisioning operation: a brand new
 * organization cannot authenticate with one of its own API keys yet, so
 * creation does not depend on an authenticated tenant.</p>
 *
 * <p>The service returns domain results ({@link OrganizationResponse} and
 * {@link Page}) and never leaks {@link ResponseEntity} or {@code ApiResponse}
 * concerns.</p>
 */
@Service
@RequiredArgsConstructor
public class OrganizationService {

    /**
     * Machine readable code returned when an organization tries to access
     * another organization.
     */
    public static final String ORGANIZATION_ACCESS_DENIED = "ORGANIZATION_ACCESS_DENIED";

    /**
     * Machine readable code returned when an organization email is already
     * used by another non deleted organization.
     */
    public static final String ORGANIZATION_EMAIL_ALREADY_EXISTS = "ORGANIZATION_EMAIL_ALREADY_EXISTS";

    /**
     * Machine readable code returned when the timezone is not a valid IANA
     * timezone.
     */
    public static final String INVALID_TIMEZONE = "INVALID_TIMEZONE";

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;

    /**
     * Provision a new organization.
     *
     * @param request creation payload
     * @return created organization
     */
    @Transactional
    public OrganizationResponse createOrganization(CreateOrganizationRequest request) {
        validateTimezone(request.getTimezone());
        ensureEmailAvailable(request.getEmail(), null);
        Organization organization = organizationMapper.toEntity(request);
        return organizationMapper.toResponse(organizationRepository.save(organization));
    }

    /**
     * Get a single organization by id.
     *
     * @param currentOrganizationId authenticated tenant id
     * @param organizationId        requested organization id
     * @return requested organization
     * @throws OrganizationNotFoundException when the organization is missing
     * @throws ForbiddenException            when the organization belongs to another tenant
     */
    @Transactional(readOnly = true)
    public OrganizationResponse getOrganizationById(Long currentOrganizationId, Long organizationId) {
        return organizationMapper.toResponse(findOrganization(currentOrganizationId, organizationId));
    }

    /**
     * Page through non deleted organizations.
     *
     * <p>The organization directory is a platform level concern; when a
     * platform admin authentication flow is introduced this endpoint is the
     * natural candidate to be restricted to it.</p>
     *
     * @param pageable pagination and sorting
     * @return page of organizations
     */
    @Transactional(readOnly = true)
    public Page<OrganizationResponse> getOrganizations(Pageable pageable) {
        return organizationRepository.findAllActiveOrganizations(pageable)
                .map(organizationMapper::toResponse);
    }

    /**
     * Search non deleted organizations by keyword.
     *
     * @param keyword  search keyword; blank returns all non deleted organizations
     * @param pageable pagination and sorting
     * @return page of matching organizations
     */
    @Transactional(readOnly = true)
    public Page<OrganizationResponse> searchOrganizations(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return getOrganizations(pageable);
        }
        return organizationRepository.searchOrganizations(keyword.trim(), pageable)
                .map(organizationMapper::toResponse);
    }

    /**
     * Update the profile of an organization.
     *
     * @param currentOrganizationId authenticated tenant id
     * @param organizationId        requested organization id
     * @param request               update payload
     * @return updated organization
     */
    @Transactional
    public OrganizationResponse updateOrganization(Long currentOrganizationId, Long organizationId,
                                                   UpdateOrganizationRequest request) {
        validateTimezone(request.getTimezone());
        Organization organization = findOrganization(currentOrganizationId, organizationId);
        ensureEmailAvailable(request.getEmail(), organizationId);
        organizationMapper.updateEntity(organization, request);
        return organizationMapper.toResponse(organizationRepository.save(organization));
    }

    /**
     * Activate an organization.
     *
     * @param currentOrganizationId authenticated tenant id
     * @param organizationId        requested organization id
     * @return activated organization
     */
    @Transactional
    public OrganizationResponse activateOrganization(Long currentOrganizationId, Long organizationId) {
        Organization organization = findOrganization(currentOrganizationId, organizationId);
        organization.activate();
        return organizationMapper.toResponse(organizationRepository.save(organization));
    }

    /**
     * Deactivate an organization without deleting it.
     *
     * @param currentOrganizationId authenticated tenant id
     * @param organizationId        requested organization id
     * @return deactivated organization
     */
    @Transactional
    public OrganizationResponse deactivateOrganization(Long currentOrganizationId, Long organizationId) {
        Organization organization = findOrganization(currentOrganizationId, organizationId);
        organization.deactivate();
        return organizationMapper.toResponse(organizationRepository.save(organization));
    }

    /**
     * Soft delete an organization. The record is never physically removed.
     *
     * @param currentOrganizationId authenticated tenant id
     * @param organizationId        requested organization id
     */
    @Transactional
    public void deleteOrganization(Long currentOrganizationId, Long organizationId) {
        Organization organization = findOrganization(currentOrganizationId, organizationId);
        organization.softDelete();
        organizationRepository.save(organization);
    }

    /**
     * Load a non deleted organization and verify it belongs to the
     * authenticated tenant.
     *
     * @param currentOrganizationId authenticated tenant id
     * @param organizationId        requested organization id
     * @return matching organization
     */
    private Organization findOrganization(Long currentOrganizationId, Long organizationId) {
        Organization organization = organizationRepository.findByIdAndNotDeleted(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        if (!organization.getId().equals(currentOrganizationId)) {
            throw new ForbiddenException(
                    "Access denied to the requested organization", ORGANIZATION_ACCESS_DENIED);
        }
        return organization;
    }

    /**
     * Ensure the email is not already used by another non deleted
     * organization. Soft deleted organizations do not block reuse.
     *
     * @param email      candidate email
     * @param excludingId organization id to exclude (own id on update), or {@code null}
     */
    private void ensureEmailAvailable(String email, Long excludingId) {
        organizationRepository.findByEmail(email).ifPresent(existing -> {
            if (excludingId == null || !existing.getId().equals(excludingId)) {
                throw new DuplicateResourceException(
                        "An organization with email " + email + " already exists",
                        ORGANIZATION_EMAIL_ALREADY_EXISTS);
            }
        });
    }

    /**
     * Reject timezones that are not valid IANA identifiers.
     *
     * @param timezone candidate timezone, may be {@code null} or blank
     */
    private void validateTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return;
        }
        if (!ZoneId.getAvailableZoneIds().contains(timezone)) {
            throw new BadRequestException("Invalid timezone: " + timezone, INVALID_TIMEZONE);
        }
    }
}
