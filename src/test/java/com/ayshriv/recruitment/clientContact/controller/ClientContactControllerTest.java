package com.ayshriv.recruitment.clientContact.controller;

import com.ayshriv.recruitment.apiKey.security.ApiKeyAuthenticationEntryPoint;
import com.ayshriv.recruitment.apiKey.security.ApiKeyPrincipal;
import com.ayshriv.recruitment.apiKey.service.ApiKeyService;
import com.ayshriv.recruitment.clientContact.dto.response.ClientContactResponse;
import com.ayshriv.recruitment.clientContact.exception.ClientContactAccessDeniedException;
import com.ayshriv.recruitment.clientContact.exception.ClientContactNotFoundException;
import com.ayshriv.recruitment.clientContact.service.ClientContactService;
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

@WebMvcTest(controllers = ClientContactController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, ApiKeyAuthenticationEntryPoint.class, SecurityContextService.class,
        AppProperties.class})
class ClientContactControllerTest {

    private static final String API_KEY = "test-api-key";

    private static final String CREATE_BODY = """
            {
                "firstName": "Sarah",
                "lastName": "Smith",
                "email": "sarah@acme.com",
                "phone": "+911234567890",
                "jobTitle": "HR Manager",
                "department": "Human Resources",
                "notes": "Primary hiring contact"
            }
            """;

    private static final String UPDATE_BODY = """
            {
                "firstName": "Sara",
                "lastName": "Smith",
                "email": "sara@acme.com",
                "phone": "+910987654321",
                "jobTitle": "Senior HR Manager",
                "department": "People Operations",
                "notes": "Senior contact"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClientContactService contactService;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @BeforeEach
    void authenticate() {
        when(apiKeyService.authenticate(API_KEY)).thenReturn(new UsernamePasswordAuthenticationToken(
                new ApiKeyPrincipal(1L, 1L, "org-a-key"), null,
                List.of(new SimpleGrantedAuthority("ROLE_API_KEY"))));
    }

    private ClientContactResponse contactResponse() {
        return ClientContactResponse.builder()
                .id(10L)
                .firstName("Sarah")
                .lastName("Smith")
                .email("sarah@acme.com")
                .phone("+911234567890")
                .jobTitle("HR Manager")
                .department("Human Resources")
                .notes("Primary hiring contact")
                .isActive(true)
                .createdOn(LocalDateTime.of(2026, 1, 1, 10, 0))
                .updatedOn(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();
    }

    @Test
    void createReturns201WithStandardEnvelope() throws Exception {
        when(contactService.createContact(eq(1L), eq(5L), any())).thenReturn(contactResponse());

        mockMvc.perform(post("/api/v1/clients/5/contacts")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Client contact created successfully"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.firstName").value("Sarah"))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.path").value("/api/v1/clients/5/contacts"));

        verify(contactService).createContact(eq(1L), eq(5L), any());
    }

    @Test
    void createResponseNeverExposesClientOrOrganization() throws Exception {
        when(contactService.createContact(eq(1L), eq(5L), any())).thenReturn(contactResponse());

        mockMvc.perform(post("/api/v1/clients/5/contacts")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.client").doesNotExist())
                .andExpect(jsonPath("$.data.organization").doesNotExist())
                .andExpect(jsonPath("$.data.isDeleted").doesNotExist());
    }

    @Test
    void createReturns400ForValidationErrors() throws Exception {
        mockMvc.perform(post("/api/v1/clients/5/contacts")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"lastName\":\"\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.firstName").value("First name is required"))
                .andExpect(jsonPath("$.error.details.lastName").value("Last name is required"))
                .andExpect(jsonPath("$.error.details.email").value("Invalid email address"));
    }

    @Test
    void listReturnsPaginatedResponseWithMetadata() throws Exception {
        when(contactService.getContacts(eq(1L), eq(5L), any())).thenReturn(
                new PageImpl<>(List.of(contactResponse()), PageRequest.of(0, 20), 25));

        mockMvc.perform(get("/api/v1/clients/5/contacts").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].email").value("sarah@acme.com"))
                .andExpect(jsonPath("$.metadata.totalElements").value(25))
                .andExpect(jsonPath("$.metadata.totalPages").value(2))
                .andExpect(jsonPath("$.metadata.hasNext").value(true));
    }

    @Test
    void searchReturnsPaginatedResults() throws Exception {
        when(contactService.searchContacts(eq(1L), eq(5L), eq("sarah"), any())).thenReturn(
                new PageImpl<>(List.of(contactResponse()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/clients/5/contacts/search")
                        .param("keyword", "sarah")
                        .header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].jobTitle").value("HR Manager"))
                .andExpect(jsonPath("$.metadata.totalElements").value(1));
    }

    @Test
    void getReturnsContactAndUsesAuthenticatedTenant() throws Exception {
        when(contactService.getContact(1L, 5L, 10L)).thenReturn(contactResponse());

        mockMvc.perform(get("/api/v1/clients/5/contacts/10").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Client contact retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(10));

        verify(contactService).getContact(1L, 5L, 10L);
    }

    @Test
    void getReturns404ForMissingContact() throws Exception {
        when(contactService.getContact(1L, 5L, 99L)).thenThrow(new ClientContactNotFoundException(99L));

        mockMvc.perform(get("/api/v1/clients/5/contacts/99").header("X-API-KEY", API_KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CLIENT_CONTACT_NOT_FOUND"));
    }

    @Test
    void getReturns403ForCrossTenantContact() throws Exception {
        when(contactService.getContact(1L, 5L, 10L)).thenThrow(new ClientContactAccessDeniedException());

        mockMvc.perform(get("/api/v1/clients/5/contacts/10").header("X-API-KEY", API_KEY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CLIENT_CONTACT_ACCESS_DENIED"));

        verify(contactService).getContact(1L, 5L, 10L);
    }

    @Test
    void updateReturnsUpdatedContact() throws Exception {
        when(contactService.updateContact(eq(1L), eq(5L), eq(10L), any())).thenReturn(contactResponse());

        mockMvc.perform(put("/api/v1/clients/5/contacts/10")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Client contact updated successfully"))
                .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    void updateReturns400ForValidationErrors() throws Exception {
        mockMvc.perform(put("/api/v1/clients/5/contacts/10")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"email\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.firstName").value("First name is required"))
                .andExpect(jsonPath("$.error.details.email").value("Email is required"));
    }

    @Test
    void updateReturns403ForCrossTenantContact() throws Exception {
        doThrow(new ClientContactAccessDeniedException())
                .when(contactService).updateContact(eq(1L), eq(5L), eq(10L), any());

        mockMvc.perform(put("/api/v1/clients/5/contacts/10")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLIENT_CONTACT_ACCESS_DENIED"));
    }

    @Test
    void activateReturnsActivatedContact() throws Exception {
        when(contactService.activateContact(1L, 5L, 10L)).thenReturn(contactResponse());

        mockMvc.perform(patch("/api/v1/clients/5/contacts/10/activate").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Client contact activated successfully"))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    void deactivateReturnsDeactivatedContact() throws Exception {
        ClientContactResponse inactive = contactResponse();
        inactive.setActive(false);
        when(contactService.deactivateContact(1L, 5L, 10L)).thenReturn(inactive);

        mockMvc.perform(patch("/api/v1/clients/5/contacts/10/deactivate").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Client contact deactivated successfully"))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/clients/5/contacts/10").header("X-API-KEY", API_KEY))
                .andExpect(status().isNoContent());
        verify(contactService).deleteContact(1L, 5L, 10L);
    }

    @Test
    void deleteReturns403ForCrossTenantContact() throws Exception {
        doThrow(new ForbiddenException("Access denied to the requested client contact",
                "CLIENT_CONTACT_ACCESS_DENIED"))
                .when(contactService).deleteContact(1L, 5L, 10L);

        mockMvc.perform(delete("/api/v1/clients/5/contacts/10").header("X-API-KEY", API_KEY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CLIENT_CONTACT_ACCESS_DENIED"));
    }

    @Test
    void unauthenticatedProtectedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/clients/5/contacts/10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("API_KEY_REQUIRED"));
    }
}