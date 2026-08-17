package com.ayshriv.recruitment.organization.mapper;

import com.ayshriv.recruitment.organization.dto.request.CreateOrganizationRequest;
import com.ayshriv.recruitment.organization.dto.request.UpdateOrganizationRequest;
import com.ayshriv.recruitment.organization.dto.response.OrganizationResponse;
import com.ayshriv.recruitment.organization.entity.Organization;
import org.springframework.stereotype.Component;

/**
 * Converts between organization request/response DTOs and the JPA entity.
 *
 * <p>Mapping is simple and deterministic. Controllers and services never
 * perform entity mapping themselves.</p>
 */
@Component
public class OrganizationMapper {

    /**
     * Map an entity into its response representation.
     *
     * @param organization source entity
     * @return response DTO
     */
    public OrganizationResponse toResponse(Organization organization) {
        return OrganizationResponse.builder()
                .id(organization.getId())
                .name(organization.getName())
                .legalName(organization.getLegalName())
                .email(organization.getEmail())
                .phone(organization.getPhone())
                .website(organization.getWebsite())
                .description(organization.getDescription())
                .industry(organization.getIndustry())
                .country(organization.getCountry())
                .state(organization.getState())
                .city(organization.getCity())
                .timezone(organization.getTimezone())
                .isActive(organization.isActive())
                .createdOn(organization.getCreatedOn())
                .updatedOn(organization.getUpdatedOn())
                .build();
    }

    /**
     * Build a new entity from a creation request.
     *
     * @param request creation payload
     * @return transient entity
     */
    public Organization toEntity(CreateOrganizationRequest request) {
        Organization organization = new Organization();
        organization.setName(request.getName());
        organization.setLegalName(request.getLegalName());
        organization.setEmail(request.getEmail());
        organization.setPhone(request.getPhone());
        organization.setWebsite(request.getWebsite());
        organization.setDescription(request.getDescription());
        organization.setIndustry(request.getIndustry());
        organization.setCountry(request.getCountry());
        organization.setState(request.getState());
        organization.setCity(request.getCity());
        organization.setTimezone(request.getTimezone());
        return organization;
    }

    /**
     * Apply updatable profile fields from an update request onto an existing
     * entity. Immutable fields (id, timestamps, active, deleted) are never
     * touched.
     *
     * @param organization target entity
     * @param request      update payload
     */
    public void updateEntity(Organization organization, UpdateOrganizationRequest request) {
        organization.setName(request.getName());
        organization.setLegalName(request.getLegalName());
        organization.setEmail(request.getEmail());
        organization.setPhone(request.getPhone());
        organization.setWebsite(request.getWebsite());
        organization.setDescription(request.getDescription());
        organization.setIndustry(request.getIndustry());
        organization.setCountry(request.getCountry());
        organization.setState(request.getState());
        organization.setCity(request.getCity());
        organization.setTimezone(request.getTimezone());
    }
}
