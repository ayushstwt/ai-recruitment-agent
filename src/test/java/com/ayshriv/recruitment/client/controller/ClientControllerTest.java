package com.ayshriv.recruitment.client.controller;

import com.ayshriv.recruitment.apiKey.security.ApiKeyAuthenticationEntryPoint;
import com.ayshriv.recruitment.apiKey.security.ApiKeyPrincipal;
import com.ayshriv.recruitment.apiKey.service.ApiKeyService;
import com.ayshriv.recruitment.client.dto.response.ClientResponse;
import com.ayshriv.recruitment.client.dto.response.ClientSummaryResponse;
import com.ayshriv.recruitment.client.entity.CompanySize;
import com.ayshriv.recruitment.client.exception.ClientAccessDeniedException;
import com.ayshriv.recruitment.client.exception.ClientNotFoundException;
import com.ayshriv.recruitment.client.service.ClientService;
import com.ayshriv.recruitment.common.config.AppProperties;
import com.ayshriv.recruitment.common.exception.ForbiddenException;
import com.ayshriv.recruitment.common.security.SecurityConfig;
import com.ayshriv.recruitment.common.security.SecurityContextService;
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

@WebMvcTest(controllers = ClientController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, ApiKeyAuthenticationEntryPoint.class, SecurityContextService.class,
        AppProperties.class})
class ClientControllerTest {

    private static final String API_KEY = "test-api-key";

    private static final String CREATE_BODY = """
            {
                "companyName": "Acme Technologies",
                "legalName": "Acme Technologies Pvt Ltd",
                "email": "hr@acme.com",
                "phone": "+911234567890",
                "website": "https://acme.com",
                "industry": "Technology",
                "companySize": "MEDIUM",
                "country": "India",
                "state": "Uttar Pradesh",
                "city": "Kanpur",
                "address": "Business Park",
                "postalCode": "208001",
                "timezone": "Asia/Kolkata",
                "description": "Technology product company",
                "notes": "Priority client"
            }
            """;

    private static final String UPDATE_BODY = """
            {
                "companyName": "Acme Global",
                "legalName": "Acme Global Pvt Ltd",
                "email": "careers@acme.com",
                "phone": "+910987654321",
                "website": "https://www.acme.com",
                "industry": "Enterprise Software",
                "companySize": "LARGE",
                "country": "India",
                "state": "Maharashtra",
                "city": "Mumbai",
                "address": "Tower A",
                "postalCode": "400001",
                "timezone": "Asia/Kolkata",
                "description": "Global product company",
                "notes": "Strategic account"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientService clientService;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @BeforeEach
    void authenticate() {
        when(apiKeyService.authenticate(API_KEY)).thenReturn(new UsernamePasswordAuthenticationToken(
                new ApiKeyPrincipal(1L, 1L, "org-a-key"), null,
                List.of(new SimpleGrantedAuthority("ROLE_API_KEY"))));
    }

    private ClientResponse clientResponse() {
        return ClientResponse.builder()
                .id(10L)
                .clientCode("CLI-000010")
                .companyName("Acme Technologies")
                .legalName("Acme Technologies Pvt Ltd")
                .email("hr@acme.com")
                .phone("+911234567890")
                .website("https://acme.com")
                .industry("Technology")
                .companySize(CompanySize.MEDIUM)
                .country("India")
                .state("Uttar Pradesh")
                .city("Kanpur")
                .address("Business Park")
                .postalCode("208001")
                .timezone("Asia/Kolkata")
                .description("Technology product company")
                .notes("Priority client")
                .isActive(true)
                .createdOn(LocalDateTime.of(2026, 1, 1, 10, 0))
                .updatedOn(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();
    }

    private ClientSummaryResponse summaryResponse() {
        return ClientSummaryResponse.builder()
                .id(10L)
                .clientCode("CLI-000010")
                .companyName("Acme Technologies")
                .industry("Technology")
                .isActive(true)
                .build();
    }

    @Test
    void createReturns201WithStandardEnvelope() throws Exception {
        when(clientService.createClient(eq(1L), any())).thenReturn(clientResponse());

        mockMvc.perform(post("/api/v1/clients")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Client created successfully"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.clientCode").value("CLI-000010"))
                .andExpect(jsonPath("$.data.companyName").value("Acme Technologies"))
                .andExpect(jsonPath("$.data.companySize").value("MEDIUM"))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.metadata").isEmpty())
                .andExpect(jsonPath("$.error").isEmpty())
                .andExpect(jsonPath("$.path").value("/api/v1/clients"));

        verify(clientService).createClient(eq(1L), any());
    }

    @Test
    void createResponseNeverExposesOrganizationOrContacts() throws Exception {
        when(clientService.createClient(eq(1L), any())).thenReturn(clientResponse());

        mockMvc.perform(post("/api/v1/clients")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.organization").doesNotExist())
                .andExpect(jsonPath("$.data.contacts").doesNotExist())
                .andExpect(jsonPath("$.data.isDeleted").doesNotExist());
    }

    @Test
    void createIgnoresClientProvidedClientCodeAndOrganizationId() throws Exception {
        when(clientService.createClient(eq(1L), any())).thenReturn(clientResponse());

        mockMvc.perform(post("/api/v1/clients")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "clientCode": "CLI-999999",
                                    "organizationId": 99,
                                    "companyName": "Acme Technologies",
                                    "email": "hr@acme.com"
                                }
                                """))
                .andExpect(status().isCreated());

        verify(clientService).createClient(eq(1L), any());
    }

    @Test
    void createReturns400ForValidationErrors() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.companyName").value("Company name is required"))
                .andExpect(jsonPath("$.error.details.email").value("Invalid email address"));
    }

    @Test
    void listReturnsPaginatedResponseWithMetadata() throws Exception {
        when(clientService.getClients(eq(1L), any())).thenReturn(
                new PageImpl<>(List.of(clientResponse()), PageRequest.of(0, 20), 25));

        mockMvc.perform(get("/api/v1/clients").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].clientCode").value("CLI-000010"))
                .andExpect(jsonPath("$.data[0].organization").doesNotExist())
                .andExpect(jsonPath("$.metadata.page").value(0))
                .andExpect(jsonPath("$.metadata.size").value(20))
                .andExpect(jsonPath("$.metadata.totalElements").value(25))
                .andExpect(jsonPath("$.metadata.totalPages").value(2))
                .andExpect(jsonPath("$.metadata.hasNext").value(true))
                .andExpect(jsonPath("$.metadata.hasPrevious").value(false));
    }

    @Test
    void searchReturnsPaginatedSummaryResults() throws Exception {
        when(clientService.searchClients(eq(1L), eq("acme"), any())).thenReturn(
                new PageImpl<>(List.of(summaryResponse()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/clients/search")
                        .param("keyword", "acme")
                        .header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].companyName").value("Acme Technologies"))
                .andExpect(jsonPath("$.data[0].industry").value("Technology"))
                .andExpect(jsonPath("$.data[0].email").doesNotExist())
                .andExpect(jsonPath("$.metadata.totalElements").value(1));
    }

    @Test
    void getReturnsClientAndUsesAuthenticatedTenant() throws Exception {
        when(clientService.getClientById(1L, 10L)).thenReturn(clientResponse());

        mockMvc.perform(get("/api/v1/clients/10").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Client retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(10));

        verify(clientService).getClientById(1L, 10L);
    }

    @Test
    void getReturns404ForMissingClient() throws Exception {
        when(clientService.getClientById(1L, 99L)).thenThrow(new ClientNotFoundException(99L));

        mockMvc.perform(get("/api/v1/clients/99").header("X-API-KEY", API_KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CLIENT_NOT_FOUND"));
    }

    @Test
    void getReturns403ForCrossTenantClient() throws Exception {
        when(clientService.getClientById(1L, 2L)).thenThrow(new ClientAccessDeniedException());

        mockMvc.perform(get("/api/v1/clients/2").header("X-API-KEY", API_KEY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CLIENT_ACCESS_DENIED"));

        verify(clientService).getClientById(1L, 2L);
    }

    @Test
    void updateReturnsUpdatedClient() throws Exception {
        when(clientService.updateClient(eq(1L), eq(10L), any())).thenReturn(clientResponse());

        mockMvc.perform(put("/api/v1/clients/10")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Client updated successfully"))
                .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    void updateReturns400ForValidationErrors() throws Exception {
        mockMvc.perform(put("/api/v1/clients/10")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"\",\"email\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.companyName").value("Company name is required"))
                .andExpect(jsonPath("$.error.details.email").value("Email is required"));
    }

    @Test
    void updateReturns403ForCrossTenantClient() throws Exception {
        doThrow(new ClientAccessDeniedException())
                .when(clientService).updateClient(eq(1L), eq(2L), any());

        mockMvc.perform(put("/api/v1/clients/2")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLIENT_ACCESS_DENIED"));
    }

    @Test
    void activateReturnsActivatedClient() throws Exception {
        when(clientService.activateClient(1L, 10L)).thenReturn(clientResponse());

        mockMvc.perform(patch("/api/v1/clients/10/activate").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Client activated successfully"))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    void deactivateReturnsDeactivatedClient() throws Exception {
        ClientResponse inactive = clientResponse();
        inactive.setActive(false);
        when(clientService.deactivateClient(1L, 10L)).thenReturn(inactive);

        mockMvc.perform(patch("/api/v1/clients/10/deactivate").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Client deactivated successfully"))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/clients/10").header("X-API-KEY", API_KEY))
                .andExpect(status().isNoContent());
        verify(clientService).deleteClient(1L, 10L);
    }

    @Test
    void deleteReturns403ForCrossTenantClient() throws Exception {
        doThrow(new ForbiddenException("Access denied to the requested client", "CLIENT_ACCESS_DENIED"))
                .when(clientService).deleteClient(1L, 2L);

        mockMvc.perform(delete("/api/v1/clients/2").header("X-API-KEY", API_KEY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLIENT_ACCESS_DENIED"));
    }

    @Test
    void unauthenticatedProtectedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/clients/10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("API_KEY_REQUIRED"));
    }
}