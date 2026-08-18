package com.ayshriv.recruitment.client;

import com.ayshriv.recruitment.apiKey.security.ApiKeyAuthenticationEntryPoint;
import com.ayshriv.recruitment.apiKey.security.ApiKeyPrincipal;
import com.ayshriv.recruitment.apiKey.service.ApiKeyService;
import com.ayshriv.recruitment.client.controller.ClientController;
import com.ayshriv.recruitment.client.entity.Client;
import com.ayshriv.recruitment.client.mapper.ClientMapper;
import com.ayshriv.recruitment.client.repository.ClientRepository;
import com.ayshriv.recruitment.client.service.ClientCodeGenerator;
import com.ayshriv.recruitment.client.service.ClientService;
import com.ayshriv.recruitment.common.config.AppProperties;
import com.ayshriv.recruitment.common.security.SecurityConfig;
import com.ayshriv.recruitment.common.security.SecurityContextService;
import com.ayshriv.recruitment.organization.entity.Organization;
import com.ayshriv.recruitment.organization.repository.OrganizationRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mandatory tenant isolation test for clients.
 *
 * <p>Organization A and Organization B exist. Client A belongs to organization
 * A and client B belongs to organization B. API key A authenticates as
 * organization A. Every attempt to reach client B through an id based endpoint
 * must fail with {@code 403 FORBIDDEN} and the {@code CLIENT_ACCESS_DENIED}
 * error code.</p>
 *
 * <p>The real {@link ClientService} is wired into the web slice so the
 * isolation decision is produced by the actual service logic, not by a
 * mock.</p>
 */
@WebMvcTest(controllers = ClientController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, ApiKeyAuthenticationEntryPoint.class, SecurityContextService.class,
        AppProperties.class, ClientService.class, ClientMapper.class, ClientCodeGenerator.class})
class ClientTenantIsolationTest {

    private static final String API_KEY_A = "test-api-key-org-a";

    private static final String UPDATE_BODY = """
            {
                "companyName": "Acme Global",
                "email": "careers@acme.com",
                "industry": "Enterprise Software",
                "companySize": "LARGE",
                "country": "India",
                "city": "Mumbai"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @MockitoBean
    private ClientRepository clientRepository;

    @MockitoBean
    private OrganizationRepository organizationRepository;

    @Test
    void organizationACannotGetClientB() throws Exception {
        mockAuthentication();
        mockClientB();

        mockMvc.perform(get("/api/v1/clients/2").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CLIENT_ACCESS_DENIED"));
    }

    @Test
    void organizationACannotUpdateClientB() throws Exception {
        mockAuthentication();
        mockClientB();

        mockMvc.perform(put("/api/v1/clients/2")
                        .header("X-API-KEY", API_KEY_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLIENT_ACCESS_DENIED"));
    }

    @Test
    void organizationACannotDeleteClientB() throws Exception {
        mockAuthentication();
        mockClientB();

        mockMvc.perform(delete("/api/v1/clients/2").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLIENT_ACCESS_DENIED"));
    }

    @Test
    void organizationACannotActivateClientB() throws Exception {
        mockAuthentication();
        mockClientB();

        mockMvc.perform(patch("/api/v1/clients/2/activate").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLIENT_ACCESS_DENIED"));
    }

    @Test
    void organizationACannotDeactivateClientB() throws Exception {
        mockAuthentication();
        mockClientB();

        mockMvc.perform(patch("/api/v1/clients/2/deactivate").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLIENT_ACCESS_DENIED"));
    }

    @Test
    void organizationACanAccessItsOwnClient() throws Exception {
        mockAuthentication();
        when(clientRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(client(1L, 1L)));

        mockMvc.perform(get("/api/v1/clients/1").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.companyName").value("Acme Technologies"))
                .andExpect(jsonPath("$.data.clientCode").value("CLI-000001"));
    }

    @Test
    void missingClientReturns404() throws Exception {
        mockAuthentication();
        when(clientRepository.findByIdAndNotDeleted(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/clients/99").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CLIENT_NOT_FOUND"));
    }

    private void mockAuthentication() {
        when(apiKeyService.authenticate(API_KEY_A)).thenReturn(new UsernamePasswordAuthenticationToken(
                new ApiKeyPrincipal(1L, 1L, "org-a-key"), null,
                List.of(new SimpleGrantedAuthority("ROLE_API_KEY"))));
    }

    private void mockClientB() {
        when(clientRepository.findByIdAndNotDeleted(2L)).thenReturn(Optional.of(client(2L, 2L)));
    }

    private Client client(Long id, Long organizationId) {
        Client client = new Client();
        client.setId(id);
        client.setClientCode("CLI-" + String.format("%06d", id));
        client.setCompanyName("Acme Technologies");
        client.setEmail("hr@acme.com");
        client.setIndustry("Technology");
        client.setOrganization(new Organization(organizationId));
        client.setActive(true);
        client.setDeleted(false);
        client.setCreatedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
        client.setUpdatedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
        return client;
    }
}