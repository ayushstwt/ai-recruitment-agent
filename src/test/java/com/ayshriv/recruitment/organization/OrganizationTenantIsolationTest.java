package com.ayshriv.recruitment.organization;

import com.ayshriv.recruitment.apiKey.security.ApiKeyAuthenticationEntryPoint;
import com.ayshriv.recruitment.apiKey.security.ApiKeyPrincipal;
import com.ayshriv.recruitment.apiKey.service.ApiKeyService;
import com.ayshriv.recruitment.common.config.AppProperties;
import com.ayshriv.recruitment.common.security.SecurityConfig;
import com.ayshriv.recruitment.common.security.SecurityContextService;
import com.ayshriv.recruitment.organization.controller.OrganizationController;
import com.ayshriv.recruitment.organization.entity.Organization;
import com.ayshriv.recruitment.organization.mapper.OrganizationMapper;
import com.ayshriv.recruitment.organization.repository.OrganizationRepository;
import com.ayshriv.recruitment.organization.service.OrganizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mandatory tenant isolation test.
 *
 * <p>Organization A and Organization B exist. API key A authenticates as
 * organization A. Every attempt to reach organization B through an id based
 * endpoint must fail with {@code 403 FORBIDDEN} and the
 * {@code ORGANIZATION_ACCESS_DENIED} error code.</p>
 *
 * <p>The real {@link OrganizationService} is wired into the web slice so the
 * isolation decision is produced by the actual service logic, not by a
 * mock.</p>
 */
@WebMvcTest(controllers = OrganizationController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, ApiKeyAuthenticationEntryPoint.class, SecurityContextService.class,
        AppProperties.class, OrganizationService.class, OrganizationMapper.class})
class OrganizationTenantIsolationTest {

    private static final String API_KEY_A = "test-api-key-org-a";

    private static final String UPDATE_BODY = """
            {
                "name": "Organization B",
                "email": "admin@orgb.com"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @MockitoBean
    private OrganizationRepository organizationRepository;

    @Test
    void organizationACannotGetOrganizationB() throws Exception {
        mockAuthentication();
        mockOrganizationB();

        mockMvc.perform(get("/api/v1/organizations/2").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ORGANIZATION_ACCESS_DENIED"));
    }

    @Test
    void organizationACannotUpdateOrganizationB() throws Exception {
        mockAuthentication();
        mockOrganizationB();

        mockMvc.perform(put("/api/v1/organizations/2")
                        .header("X-API-KEY", API_KEY_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ORGANIZATION_ACCESS_DENIED"));
    }

    @Test
    void organizationACannotDeleteOrganizationB() throws Exception {
        mockAuthentication();
        mockOrganizationB();

        mockMvc.perform(delete("/api/v1/organizations/2").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ORGANIZATION_ACCESS_DENIED"));
    }

    @Test
    void organizationACanAccessItsOwnOrganization() throws Exception {
        mockAuthentication();
        Organization organizationA = organization(1L, "Organization A", "admin@orga.com");
        when(organizationRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(organizationA));

        mockMvc.perform(get("/api/v1/organizations/1").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    private void mockAuthentication() {
        when(apiKeyService.authenticate(API_KEY_A)).thenReturn(new UsernamePasswordAuthenticationToken(
                new ApiKeyPrincipal(1L, 1L, "org-a-key"), null,
                List.of(new SimpleGrantedAuthority("ROLE_API_KEY"))));
    }

    private void mockOrganizationB() {
        when(organizationRepository.findByIdAndNotDeleted(2L))
                .thenReturn(Optional.of(organization(2L, "Organization B", "admin@orgb.com")));
    }

    private Organization organization(Long id, String name, String email) {
        Organization organization = new Organization();
        organization.setId(id);
        organization.setName(name);
        organization.setEmail(email);
        organization.setLegalName(name);
        organization.setActive(true);
        organization.setDeleted(false);
        organization.setCreatedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
        organization.setUpdatedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
        return organization;
    }
}
