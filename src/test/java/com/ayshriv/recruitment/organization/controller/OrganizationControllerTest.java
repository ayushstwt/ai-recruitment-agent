package com.ayshriv.recruitment.organization.controller;

import com.ayshriv.recruitment.apiKey.security.ApiKeyAuthenticationEntryPoint;
import com.ayshriv.recruitment.apiKey.security.ApiKeyPrincipal;
import com.ayshriv.recruitment.apiKey.service.ApiKeyService;
import com.ayshriv.recruitment.common.config.AppProperties;
import com.ayshriv.recruitment.common.exception.ForbiddenException;
import com.ayshriv.recruitment.common.security.SecurityConfig;
import com.ayshriv.recruitment.common.security.SecurityContextService;
import com.ayshriv.recruitment.organization.dto.response.OrganizationResponse;
import com.ayshriv.recruitment.organization.exception.OrganizationNotFoundException;
import com.ayshriv.recruitment.organization.service.OrganizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrganizationController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, ApiKeyAuthenticationEntryPoint.class, SecurityContextService.class,
        AppProperties.class})
class OrganizationControllerTest {

    private static final String API_KEY = "test-api-key";

    private static final String VALID_BODY = """
            {
                "name": "Acme Recruitment",
                "legalName": "Acme Recruitment Pvt Ltd",
                "email": "admin@acme.com",
                "phone": "+911234567890",
                "website": "https://acme.com",
                "description": "Recruitment agency",
                "industry": "Recruitment",
                "country": "India",
                "state": "Uttar Pradesh",
                "city": "Kanpur",
                "timezone": "Asia/Kolkata"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrganizationService organizationService;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @BeforeEach
    void authenticate() {
        when(apiKeyService.authenticate(API_KEY)).thenReturn(new UsernamePasswordAuthenticationToken(
                new ApiKeyPrincipal(1L, 1L, "prod-key"), null,
                List.of(new SimpleGrantedAuthority("ROLE_API_KEY"))));
    }

    private OrganizationResponse response(Long id) {
        return OrganizationResponse.builder()
                .id(id)
                .name("Acme Recruitment")
                .legalName("Acme Recruitment Pvt Ltd")
                .email("admin@acme.com")
                .phone("+911234567890")
                .website("https://acme.com")
                .description("Recruitment agency")
                .industry("Recruitment")
                .country("India")
                .state("Uttar Pradesh")
                .city("Kanpur")
                .timezone("Asia/Kolkata")
                .isActive(true)
                .createdOn(LocalDateTime.of(2026, 1, 1, 10, 0))
                .updatedOn(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();
    }

    @Test
    void createReturns201WithStandardEnvelope() throws Exception {
        when(organizationService.createOrganization(any())).thenReturn(response(1L));

        mockMvc.perform(post("/api/v1/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Organization created successfully"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Acme Recruitment"))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.metadata").isEmpty())
                .andExpect(jsonPath("$.error").isEmpty())
                .andExpect(jsonPath("$.path").value("/api/v1/organizations"));
    }

    @Test
    void createReturns400ForValidationErrors() throws Exception {
        mockMvc.perform(post("/api/v1/organizations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.name").value("Organization name is required"))
                .andExpect(jsonPath("$.error.details.email").value("Invalid email address"));
    }

    @Test
    void listReturnsPaginatedResponseWithMetadata() throws Exception {
        when(organizationService.getOrganizations(any())).thenReturn(
                new PageImpl<>(List.of(response(1L)), PageRequest.of(0, 20), 25));

        mockMvc.perform(get("/api/v1/organizations").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.metadata.page").value(0))
                .andExpect(jsonPath("$.metadata.size").value(20))
                .andExpect(jsonPath("$.metadata.totalElements").value(25))
                .andExpect(jsonPath("$.metadata.totalPages").value(2))
                .andExpect(jsonPath("$.metadata.hasNext").value(true))
                .andExpect(jsonPath("$.metadata.hasPrevious").value(false));
    }

    @Test
    void searchReturnsPaginatedResults() throws Exception {
        when(organizationService.searchOrganizations(eq("acme"), any())).thenReturn(
                new PageImpl<>(List.of(response(1L)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/organizations/search")
                        .param("keyword", "acme")
                        .header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].email").value("admin@acme.com"))
                .andExpect(jsonPath("$.metadata.totalElements").value(1));
    }

    @Test
    void getReturnsOrganizationAndUsesAuthenticatedTenant() throws Exception {
        when(organizationService.getOrganizationById(1L, 1L)).thenReturn(response(1L));

        mockMvc.perform(get("/api/v1/organizations/1").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Organization retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(1));

        verify(organizationService).getOrganizationById(1L, 1L);
    }

    @Test
    void getDoesNotExposeApiKeys() throws Exception {
        when(organizationService.getOrganizationById(1L, 1L)).thenReturn(response(1L));

        mockMvc.perform(get("/api/v1/organizations/1").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.apiKeys").doesNotExist());
    }

    @Test
    void getReturns404ForMissingOrganization() throws Exception {
        when(organizationService.getOrganizationById(1L, 99L))
                .thenThrow(new OrganizationNotFoundException(99L));

        mockMvc.perform(get("/api/v1/organizations/99").header("X-API-KEY", API_KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ORGANIZATION_NOT_FOUND"));
    }

    @Test
    void getReturns403ForCrossTenantOrganization() throws Exception {
        when(organizationService.getOrganizationById(1L, 2L))
                .thenThrow(new ForbiddenException("Access denied to the requested organization",
                        OrganizationService.ORGANIZATION_ACCESS_DENIED));

        mockMvc.perform(get("/api/v1/organizations/2").header("X-API-KEY", API_KEY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ORGANIZATION_ACCESS_DENIED"));
        verify(organizationService).getOrganizationById(1L, 2L);
    }

    @Test
    void updateReturnsUpdatedOrganization() throws Exception {
        when(organizationService.updateOrganization(eq(1L), eq(1L), any())).thenReturn(response(1L));

        mockMvc.perform(put("/api/v1/organizations/1")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Organization updated successfully"))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void updateReturns400ForValidationErrors() throws Exception {
        mockMvc.perform(put("/api/v1/organizations/1")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.name").value("Organization name is required"))
                .andExpect(jsonPath("$.error.details.email").value("Email is required"));
    }

    @Test
    void updateReturns403ForCrossTenantOrganization() throws Exception {
        when(organizationService.updateOrganization(eq(1L), eq(2L), any()))
                .thenThrow(new ForbiddenException("Access denied to the requested organization",
                        OrganizationService.ORGANIZATION_ACCESS_DENIED));

        mockMvc.perform(put("/api/v1/organizations/2")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ORGANIZATION_ACCESS_DENIED"));
        verify(organizationService).updateOrganization(eq(1L), eq(2L), any());
    }

    @Test
    void activateReturnsActivatedOrganization() throws Exception {
        when(organizationService.activateOrganization(1L, 1L)).thenReturn(response(1L));

        mockMvc.perform(patch("/api/v1/organizations/1/activate").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Organization activated successfully"))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    void deactivateReturnsDeactivatedOrganization() throws Exception {
        OrganizationResponse inactive = response(1L);
        inactive.setActive(false);
        when(organizationService.deactivateOrganization(1L, 1L)).thenReturn(inactive);

        mockMvc.perform(patch("/api/v1/organizations/1/deactivate").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Organization deactivated successfully"))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/organizations/1").header("X-API-KEY", API_KEY))
                .andExpect(status().isNoContent());
        verify(organizationService).deleteOrganization(1L, 1L);
    }

    @Test
    void deleteReturns403ForCrossTenantOrganization() throws Exception {
        doThrow(new ForbiddenException("Access denied to the requested organization",
                OrganizationService.ORGANIZATION_ACCESS_DENIED))
                .when(organizationService).deleteOrganization(1L, 2L);

        mockMvc.perform(delete("/api/v1/organizations/2").header("X-API-KEY", API_KEY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ORGANIZATION_ACCESS_DENIED"));
    }

    @Test
    void unauthenticatedProtectedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("API_KEY_REQUIRED"));
    }
}
