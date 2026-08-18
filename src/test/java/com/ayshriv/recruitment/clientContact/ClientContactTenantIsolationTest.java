package com.ayshriv.recruitment.clientContact;

import com.ayshriv.recruitment.apiKey.security.ApiKeyAuthenticationEntryPoint;
import com.ayshriv.recruitment.apiKey.security.ApiKeyPrincipal;
import com.ayshriv.recruitment.apiKey.service.ApiKeyService;
import com.ayshriv.recruitment.client.entity.Client;
import com.ayshriv.recruitment.client.mapper.ClientMapper;
import com.ayshriv.recruitment.client.repository.ClientRepository;
import com.ayshriv.recruitment.client.service.ClientCodeGenerator;
import com.ayshriv.recruitment.client.service.ClientService;
import com.ayshriv.recruitment.clientContact.controller.ClientContactController;
import com.ayshriv.recruitment.clientContact.entity.ClientContact;
import com.ayshriv.recruitment.clientContact.mapper.ClientContactMapper;
import com.ayshriv.recruitment.clientContact.repository.ClientContactRepository;
import com.ayshriv.recruitment.clientContact.service.ClientContactService;
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
 * Mandatory tenant isolation test for client contacts.
 *
 * <p>Organization A and Organization B exist. Client A belongs to organization
 * A and client B belongs to organization B. API key A authenticates as
 * organization A. Every attempt to reach a contact belonging to client B must
 * fail with {@code 403 FORBIDDEN} and the {@code CLIENT_CONTACT_ACCESS_DENIED}
 * error code.</p>
 *
 * <p>The real {@link ClientContactService} and {@link ClientService} are wired
 * into the web slice so the isolation decision is produced by the actual
 * service logic, not by a mock.</p>
 */
@WebMvcTest(controllers = ClientContactController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, ApiKeyAuthenticationEntryPoint.class, SecurityContextService.class,
        AppProperties.class, ClientContactService.class, ClientContactMapper.class,
        ClientService.class, ClientMapper.class, ClientCodeGenerator.class})
class ClientContactTenantIsolationTest {

    private static final String API_KEY_A = "test-api-key-org-a";

    private static final String UPDATE_BODY = """
            {
                "firstName": "Sara",
                "lastName": "Smith",
                "email": "sara@acme.com",
                "jobTitle": "Senior HR Manager"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @MockitoBean
    private ClientContactRepository contactRepository;

    @MockitoBean
    private ClientRepository clientRepository;

    @MockitoBean
    private OrganizationRepository organizationRepository;

    @Test
    void organizationACannotGetContactOfClientB() throws Exception {
        mockAuthentication();
        mockClientBContact();

        mockMvc.perform(get("/api/v1/clients/2/contacts/10").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CLIENT_CONTACT_ACCESS_DENIED"));
    }

    @Test
    void organizationACannotUpdateContactOfClientB() throws Exception {
        mockAuthentication();
        mockClientBContact();

        mockMvc.perform(put("/api/v1/clients/2/contacts/10")
                        .header("X-API-KEY", API_KEY_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLIENT_CONTACT_ACCESS_DENIED"));
    }

    @Test
    void organizationACannotDeleteContactOfClientB() throws Exception {
        mockAuthentication();
        mockClientBContact();

        mockMvc.perform(delete("/api/v1/clients/2/contacts/10").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLIENT_CONTACT_ACCESS_DENIED"));
    }

    @Test
    void organizationACannotActivateContactOfClientB() throws Exception {
        mockAuthentication();
        mockClientBContact();

        mockMvc.perform(patch("/api/v1/clients/2/contacts/10/activate").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLIENT_CONTACT_ACCESS_DENIED"));
    }

    @Test
    void organizationACannotDeactivateContactOfClientB() throws Exception {
        mockAuthentication();
        mockClientBContact();

        mockMvc.perform(patch("/api/v1/clients/2/contacts/10/deactivate").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLIENT_CONTACT_ACCESS_DENIED"));
    }

    @Test
    void organizationACanAccessItsOwnContact() throws Exception {
        mockAuthentication();
        ClientContact ownContact = contact(10L, 1L, 1L);
        when(contactRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(ownContact));
        when(contactRepository.existsByIdAndOrganization(10L, 1L)).thenReturn(true);

        mockMvc.perform(get("/api/v1/clients/1/contacts/10").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.email").value("sarah@acme.com"));
    }

    @Test
    void missingContactReturns404() throws Exception {
        mockAuthentication();
        when(contactRepository.findByIdAndNotDeleted(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/clients/1/contacts/99").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("CLIENT_CONTACT_NOT_FOUND"));
    }

    private void mockAuthentication() {
        when(apiKeyService.authenticate(API_KEY_A)).thenReturn(new UsernamePasswordAuthenticationToken(
                new ApiKeyPrincipal(1L, 1L, "org-a-key"), null,
                List.of(new SimpleGrantedAuthority("ROLE_API_KEY"))));
    }

    private void mockClientBContact() {
        when(contactRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(contact(10L, 2L, 2L)));
        when(contactRepository.existsByIdAndOrganization(10L, 1L)).thenReturn(false);
    }

    private ClientContact contact(Long id, Long clientId, Long organizationId) {
        ClientContact contact = new ClientContact();
        contact.setId(id);
        contact.setFirstName("Sarah");
        contact.setLastName("Smith");
        contact.setEmail("sarah@acme.com");
        contact.setJobTitle("HR Manager");
        Client client = new Client(clientId);
        client.setOrganization(new Organization(organizationId));
        contact.setClient(client);
        contact.setActive(true);
        contact.setDeleted(false);
        contact.setCreatedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
        contact.setUpdatedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
        return contact;
    }
}