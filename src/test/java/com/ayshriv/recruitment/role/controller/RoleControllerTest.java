package com.ayshriv.recruitment.role.controller;

import com.ayshriv.recruitment.apiKey.security.ApiKeyAuthenticationEntryPoint;
import com.ayshriv.recruitment.apiKey.security.ApiKeyPrincipal;
import com.ayshriv.recruitment.apiKey.service.ApiKeyService;
import com.ayshriv.recruitment.common.config.AppProperties;
import com.ayshriv.recruitment.common.security.SecurityConfig;
import com.ayshriv.recruitment.common.security.SecurityContextService;
import com.ayshriv.recruitment.role.dto.response.RoleResponse;
import com.ayshriv.recruitment.role.exception.RoleNotFoundException;
import com.ayshriv.recruitment.role.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RoleController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, ApiKeyAuthenticationEntryPoint.class, SecurityContextService.class,
        AppProperties.class})
class RoleControllerTest {

    private static final String API_KEY = "test-api-key";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoleService roleService;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @BeforeEach
    void authenticate() {
        when(apiKeyService.authenticate(API_KEY)).thenReturn(new UsernamePasswordAuthenticationToken(
                new ApiKeyPrincipal(1L, 1L, "org-a-key"), null,
                List.of(new SimpleGrantedAuthority("ROLE_API_KEY"))));
    }

    private RoleResponse roleResponse(Long id, String name) {
        return RoleResponse.builder()
                .id(id)
                .name(name)
                .description("A default system role")
                .isSystemRole(true)
                .build();
    }

    @Test
    void listReturnsAccessibleRoles() throws Exception {
        when(roleService.getRoles(1L)).thenReturn(
                List.of(roleResponse(1L, "AGENCY_ADMIN"), roleResponse(3L, "RECRUITER")));

        mockMvc.perform(get("/api/v1/roles").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Roles retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("AGENCY_ADMIN"))
                .andExpect(jsonPath("$.data[1].name").value("RECRUITER"));

        verify(roleService).getRoles(1L);
    }

    @Test
    void listSystemReturnsSystemRoles() throws Exception {
        when(roleService.getSystemRoles()).thenReturn(List.of(roleResponse(3L, "RECRUITER")));

        mockMvc.perform(get("/api/v1/roles/system").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("System roles retrieved successfully"))
                .andExpect(jsonPath("$.data[0].id").value(3))
                .andExpect(jsonPath("$.data[0].name").value("RECRUITER"))
                .andExpect(jsonPath("$.data[0].isSystemRole").value(true));
    }

    @Test
    void getReturnsRoleScopedToAuthenticatedTenant() throws Exception {
        when(roleService.getRoleById(1L, 3L)).thenReturn(roleResponse(3L, "RECRUITER"));

        mockMvc.perform(get("/api/v1/roles/3").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Role retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(3));

        verify(roleService).getRoleById(1L, 3L);
    }

    @Test
    void getReturns404ForMissingOrInaccessibleRole() throws Exception {
        when(roleService.getRoleById(1L, 99L)).thenThrow(new RoleNotFoundException(99L));

        mockMvc.perform(get("/api/v1/roles/99").header("X-API-KEY", API_KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ROLE_NOT_FOUND"));
    }

    @Test
    void unauthenticatedProtectedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("API_KEY_REQUIRED"));
    }
}