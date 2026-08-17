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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Spy
    private OrganizationMapper organizationMapper = new OrganizationMapper();

    @InjectMocks
    private OrganizationService organizationService;

    private Organization organization(Long id, String name, String email) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setName(name);
        organization.setLegalName(name + " Pvt Ltd");
        organization.setEmail(email);
        organization.setPhone("+911234567890");
        organization.setWebsite("https://example.com");
        organization.setDescription("Recruitment agency");
        organization.setIndustry("Recruitment");
        organization.setCountry("India");
        organization.setState("Uttar Pradesh");
        organization.setCity("Kanpur");
        organization.setTimezone("Asia/Kolkata");
        organization.setActive(true);
        organization.setDeleted(false);
        organization.setCreatedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
        organization.setUpdatedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
        return organization;
    }

    private CreateOrganizationRequest createRequest(String email) {
        return new CreateOrganizationRequest(
                "Acme Recruitment", "Acme Recruitment Pvt Ltd", email,
                "+911234567890", "https://acmerecruitment.com", "Technology recruitment agency",
                "Recruitment", "India", "Uttar Pradesh", "Kanpur", "Asia/Kolkata");
    }

    private UpdateOrganizationRequest updateRequest(String email) {
        return new UpdateOrganizationRequest(
                "Updated Recruitment Agency", "Updated Recruitment Pvt Ltd", email,
                "+911234567890", "https://updated.com", "Updated description",
                "Recruitment", "India", "Uttar Pradesh", "Kanpur", "Asia/Kolkata");
    }

    @Test
    void createOrganizationSavesAndReturnsResponse() {
        when(organizationRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationResponse response = organizationService.createOrganization(createRequest("admin@acme.com"));

        ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
        verify(organizationRepository).save(captor.capture());
        Organization saved = captor.getValue();

        assertThat(response.getName()).isEqualTo("Acme Recruitment");
        assertThat(response.getEmail()).isEqualTo("admin@acme.com");
        assertThat(response.getTimezone()).isEqualTo("Asia/Kolkata");
        assertThat(saved.getLegalName()).isEqualTo("Acme Recruitment Pvt Ltd");
    }

    @Test
    void createOrganizationRejectsDuplicateEmail() {
        when(organizationRepository.findByEmail("admin@acme.com"))
                .thenReturn(Optional.of(organization(1L, "Other", "admin@acme.com")));

        assertThatThrownBy(() -> organizationService.createOrganization(createRequest("admin@acme.com")))
                .isInstanceOf(DuplicateResourceException.class)
                .extracting(ex -> ((DuplicateResourceException) ex).getCode())
                .isEqualTo("ORGANIZATION_EMAIL_ALREADY_EXISTS");
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void createOrganizationAllowsEmailReuseWhenOnlySoftDeletedOrganizationOwnsIt() {
        when(organizationRepository.findByEmail("admin@acme.com")).thenReturn(Optional.empty());
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        organizationService.createOrganization(createRequest("admin@acme.com"));

        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    void createOrganizationRejectsInvalidTimezone() {
        CreateOrganizationRequest request = createRequest("admin@acme.com");
        request.setTimezone("Not/AZone");

        assertThatThrownBy(() -> organizationService.createOrganization(request))
                .isInstanceOf(BadRequestException.class)
                .extracting(ex -> ((BadRequestException) ex).getCode())
                .isEqualTo("INVALID_TIMEZONE");
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void getOrganizationByIdReturnsMappedOrganization() {
        Organization organization = organization(1L, "Acme", "admin@acme.com");
        when(organizationRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(organization));

        OrganizationResponse response = organizationService.getOrganizationById(1L, 1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Acme");
        assertThat(response.isActive()).isTrue();
        verify(organizationRepository).findByIdAndNotDeleted(1L);
    }

    @Test
    void getOrganizationByIdThrowsNotFound() {
        when(organizationRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.getOrganizationById(1L, 1L))
                .isInstanceOf(OrganizationNotFoundException.class)
                .extracting(ex -> ((OrganizationNotFoundException) ex).getCode())
                .isEqualTo("ORGANIZATION_NOT_FOUND");
    }

    @Test
    void getOrganizationByIdThrowsForbiddenWhenOrganizationBelongsToAnotherTenant() {
        Organization organizationB = organization(2L, "Org B", "admin@orgb.com");
        when(organizationRepository.findByIdAndNotDeleted(2L)).thenReturn(Optional.of(organizationB));

        assertThatThrownBy(() -> organizationService.getOrganizationById(1L, 2L))
                .isInstanceOf(ForbiddenException.class)
                .extracting(ex -> ((ForbiddenException) ex).getCode())
                .isEqualTo("ORGANIZATION_ACCESS_DENIED");
    }

    @Test
    void getOrganizationsReturnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Organization> page = new PageImpl<>(
                List.of(organization(1L, "Acme", "admin@acme.com")), pageable, 1);
        when(organizationRepository.findAllActiveOrganizations(pageable)).thenReturn(page);

        Page<OrganizationResponse> result = organizationService.getOrganizations(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("admin@acme.com");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void searchOrganizationsDelegatesToSearchQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Organization> page = new PageImpl<>(
                List.of(organization(1L, "Acme", "admin@acme.com")), pageable, 1);
        when(organizationRepository.searchOrganizations(eq("acme"), eq(pageable))).thenReturn(page);

        Page<OrganizationResponse> result = organizationService.searchOrganizations("acme", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(organizationRepository).searchOrganizations("acme", pageable);
    }

    @Test
    void searchOrganizationsWithBlankKeywordFallsBackToAllOrganizations() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Organization> page = new PageImpl<>(List.of(), pageable, 0);
        when(organizationRepository.findAllActiveOrganizations(pageable)).thenReturn(page);

        organizationService.searchOrganizations("   ", pageable);

        verify(organizationRepository).findAllActiveOrganizations(pageable);
        verify(organizationRepository, never()).searchOrganizations(any(), any());
    }

    @Test
    void updateOrganizationAppliesProfileChanges() {
        Organization organization = organization(1L, "Acme", "admin@acme.com");
        when(organizationRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(organization));
        when(organizationRepository.findByEmail("contact@updated.com")).thenReturn(Optional.empty());
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationResponse response =
                organizationService.updateOrganization(1L, 1L, updateRequest("contact@updated.com"));

        assertThat(response.getName()).isEqualTo("Updated Recruitment Agency");
        assertThat(response.getEmail()).isEqualTo("contact@updated.com");
        assertThat(response.getDescription()).isEqualTo("Updated description");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void updateOrganizationAllowsKeepingOwnEmail() {
        Organization organization = organization(1L, "Acme", "admin@acme.com");
        when(organizationRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(organization));
        when(organizationRepository.findByEmail("admin@acme.com")).thenReturn(Optional.of(organization));
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationResponse response =
                organizationService.updateOrganization(1L, 1L, updateRequest("admin@acme.com"));

        assertThat(response.getEmail()).isEqualTo("admin@acme.com");
    }

    @Test
    void updateOrganizationRejectsEmailUsedByAnotherOrganization() {
        Organization organization = organization(1L, "Acme", "admin@acme.com");
        when(organizationRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(organization));
        when(organizationRepository.findByEmail("taken@other.com"))
                .thenReturn(Optional.of(organization(2L, "Other", "taken@other.com")));

        assertThatThrownBy(() -> organizationService.updateOrganization(1L, 1L,
                updateRequest("taken@other.com")))
                .isInstanceOf(DuplicateResourceException.class)
                .extracting(ex -> ((DuplicateResourceException) ex).getCode())
                .isEqualTo("ORGANIZATION_EMAIL_ALREADY_EXISTS");
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void updateOrganizationThrowsForbiddenWhenTargetBelongsToAnotherTenant() {
        Organization organizationB = organization(2L, "Org B", "admin@orgb.com");
        when(organizationRepository.findByIdAndNotDeleted(2L)).thenReturn(Optional.of(organizationB));

        assertThatThrownBy(() -> organizationService.updateOrganization(1L, 2L,
                updateRequest("contact@orgb.com")))
                .isInstanceOf(ForbiddenException.class)
                .extracting(ex -> ((ForbiddenException) ex).getCode())
                .isEqualTo("ORGANIZATION_ACCESS_DENIED");
    }

    @Test
    void activateOrganizationActivates() {
        Organization organization = organization(1L, "Acme", "admin@acme.com");
        organization.setActive(false);
        when(organizationRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(organization));
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationResponse response = organizationService.activateOrganization(1L, 1L);

        assertThat(response.isActive()).isTrue();
        assertThat(organization.isActive()).isTrue();
        assertThat(organization.isDeleted()).isFalse();
    }

    @Test
    void deactivateOrganizationDeactivatesWithoutDeleting() {
        Organization organization = organization(1L, "Acme", "admin@acme.com");
        when(organizationRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(organization));
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrganizationResponse response = organizationService.deactivateOrganization(1L, 1L);

        assertThat(response.isActive()).isFalse();
        assertThat(organization.isDeleted()).isFalse();
    }

    @Test
    void deactivateOrganizationThrowsForbiddenForAnotherTenant() {
        Organization organizationB = organization(2L, "Org B", "admin@orgb.com");
        when(organizationRepository.findByIdAndNotDeleted(2L)).thenReturn(Optional.of(organizationB));

        assertThatThrownBy(() -> organizationService.deactivateOrganization(1L, 2L))
                .isInstanceOf(ForbiddenException.class)
                .extracting(ex -> ((ForbiddenException) ex).getCode())
                .isEqualTo("ORGANIZATION_ACCESS_DENIED");
    }

    @Test
    void deleteOrganizationSoftDeletes() {
        Organization organization = organization(1L, "Acme", "admin@acme.com");
        when(organizationRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(organization));
        when(organizationRepository.save(any(Organization.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        organizationService.deleteOrganization(1L, 1L);

        assertThat(organization.isDeleted()).isTrue();
        assertThat(organization.isActive()).isFalse();
        verify(organizationRepository).save(organization);
    }

    @Test
    void deleteOrganizationThrowsForbiddenForAnotherTenant() {
        Organization organizationB = organization(2L, "Org B", "admin@orgb.com");
        when(organizationRepository.findByIdAndNotDeleted(2L)).thenReturn(Optional.of(organizationB));

        assertThatThrownBy(() -> organizationService.deleteOrganization(1L, 2L))
                .isInstanceOf(ForbiddenException.class)
                .extracting(ex -> ((ForbiddenException) ex).getCode())
                .isEqualTo("ORGANIZATION_ACCESS_DENIED");
        verify(organizationRepository, never()).save(any());
    }

    @Test
    void deletedOrganizationCannotBeRetrieved() {
        when(organizationRepository.findByIdAndNotDeleted(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.getOrganizationById(1L, 999L))
                .isInstanceOf(OrganizationNotFoundException.class);
    }
}
