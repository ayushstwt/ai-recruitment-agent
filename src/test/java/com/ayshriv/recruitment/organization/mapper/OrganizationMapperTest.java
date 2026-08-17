package com.ayshriv.recruitment.organization.mapper;

import com.ayshriv.recruitment.organization.dto.request.CreateOrganizationRequest;
import com.ayshriv.recruitment.organization.dto.request.UpdateOrganizationRequest;
import com.ayshriv.recruitment.organization.dto.response.OrganizationResponse;
import com.ayshriv.recruitment.organization.entity.Organization;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrganizationMapperTest {

    private final OrganizationMapper mapper = new OrganizationMapper();

    private Organization organization() {
        Organization organization = new Organization();
        organization.setId(7L);
        organization.setName("Acme Recruitment");
        organization.setLegalName("Acme Recruitment Pvt Ltd");
        organization.setEmail("admin@acme.com");
        organization.setPhone("+911234567890");
        organization.setWebsite("https://acme.com");
        organization.setDescription("Recruitment agency");
        organization.setIndustry("Recruitment");
        organization.setCountry("India");
        organization.setState("Uttar Pradesh");
        organization.setCity("Kanpur");
        organization.setTimezone("Asia/Kolkata");
        organization.setActive(true);
        organization.setDeleted(false);
        organization.setCreatedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
        organization.setUpdatedOn(LocalDateTime.of(2026, 1, 2, 10, 0));
        return organization;
    }

    @Test
    void toResponseMapsEveryField() {
        OrganizationResponse response = mapper.toResponse(organization());

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getName()).isEqualTo("Acme Recruitment");
        assertThat(response.getLegalName()).isEqualTo("Acme Recruitment Pvt Ltd");
        assertThat(response.getEmail()).isEqualTo("admin@acme.com");
        assertThat(response.getPhone()).isEqualTo("+911234567890");
        assertThat(response.getWebsite()).isEqualTo("https://acme.com");
        assertThat(response.getDescription()).isEqualTo("Recruitment agency");
        assertThat(response.getIndustry()).isEqualTo("Recruitment");
        assertThat(response.getCountry()).isEqualTo("India");
        assertThat(response.getState()).isEqualTo("Uttar Pradesh");
        assertThat(response.getCity()).isEqualTo("Kanpur");
        assertThat(response.getTimezone()).isEqualTo("Asia/Kolkata");
        assertThat(response.isActive()).isTrue();
        assertThat(response.getCreatedOn()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0));
        assertThat(response.getUpdatedOn()).isEqualTo(LocalDateTime.of(2026, 1, 2, 10, 0));
    }

    @Test
    void toEntityMapsCreationRequest() {
        CreateOrganizationRequest request = new CreateOrganizationRequest(
                "Acme Recruitment", "Acme Recruitment Pvt Ltd", "admin@acme.com",
                "+911234567890", "https://acme.com", "Recruitment agency",
                "Recruitment", "India", "Uttar Pradesh", "Kanpur", "Asia/Kolkata");

        Organization organization = mapper.toEntity(request);

        assertThat(organization.getId()).isNull();
        assertThat(organization.getName()).isEqualTo("Acme Recruitment");
        assertThat(organization.getEmail()).isEqualTo("admin@acme.com");
        assertThat(organization.getCity()).isEqualTo("Kanpur");
    }

    @Test
    void updateEntityChangesProfileFieldsAndLeavesLifecycleAlone() {
        Organization organization = organization();
        UpdateOrganizationRequest request = new UpdateOrganizationRequest(
                "New Name", "New Legal", "new@acme.com",
                "+910987654321", "https://new.com", "New description",
                "Tech", "USA", "California", "San Francisco", "America/Los_Angeles");

        mapper.updateEntity(organization, request);

        assertThat(organization.getName()).isEqualTo("New Name");
        assertThat(organization.getLegalName()).isEqualTo("New Legal");
        assertThat(organization.getEmail()).isEqualTo("new@acme.com");
        assertThat(organization.getPhone()).isEqualTo("+910987654321");
        assertThat(organization.getWebsite()).isEqualTo("https://new.com");
        assertThat(organization.getDescription()).isEqualTo("New description");
        assertThat(organization.getIndustry()).isEqualTo("Tech");
        assertThat(organization.getCountry()).isEqualTo("USA");
        assertThat(organization.getState()).isEqualTo("California");
        assertThat(organization.getCity()).isEqualTo("San Francisco");
        assertThat(organization.getTimezone()).isEqualTo("America/Los_Angeles");
        assertThat(organization.getId()).isEqualTo(7L);
        assertThat(organization.getCreatedOn()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0));
        assertThat(organization.isActive()).isTrue();
        assertThat(organization.isDeleted()).isFalse();
    }
}
