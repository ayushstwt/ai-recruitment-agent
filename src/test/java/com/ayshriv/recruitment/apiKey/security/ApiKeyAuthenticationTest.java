package com.ayshriv.recruitment.apiKey.security;

import com.ayshriv.recruitment.apiKey.service.ApiKeyService;
import com.ayshriv.recruitment.common.config.AppProperties;
import com.ayshriv.recruitment.common.response.ApiResponse;
import com.ayshriv.recruitment.common.response.ResponseUtil;
import com.ayshriv.recruitment.common.security.SecurityConfig;
import com.ayshriv.recruitment.common.security.SecurityContextService;
import com.ayshriv.recruitment.organization.service.OrganizationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@AutoConfigureMockMvc
@Import({SecurityConfig.class, ApiKeyAuthenticationEntryPoint.class, SecurityContextService.class,
        AppProperties.class, ApiKeyAuthenticationTest.SecureController.class})
class ApiKeyAuthenticationTest {

    private static final String VALID_KEY = apiKeyFromEnv("TEST_API_KEY", "a1b2c3d4e5f60718293a4b5c6d7e8f90");
    private static final String INVALID_KEY = apiKeyFromEnv("TEST_API_KEY_INVALID", "bad0000000000000000000000000000");

    private static String apiKeyFromEnv(String name, String fallbackHex) {
        String fromEnv = System.getenv(name);
        return (fromEnv != null && !fromEnv.isBlank()) ? fromEnv : ApiKeyService.KEY_PREFIX_SEED + fallbackHex;
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @MockitoBean
    private OrganizationService organizationService;

    @RestController
    @RequestMapping("/api/v1/secure")
    static class SecureController {

        private final SecurityContextService securityContextService;

        SecureController(SecurityContextService securityContextService) {
            this.securityContextService = securityContextService;
        }

        @GetMapping("/whoami")
        public ResponseEntity<ApiResponse<Map<String, Object>>> whoami(HttpServletRequest request) {
            Map<String, Object> data = Map.of(
                    "organizationId", securityContextService.getCurrentOrganizationId(),
                    "apiKeyId", securityContextService.getCurrentApiKeyId());
            return ResponseEntity.ok(ResponseUtil.success("Authenticated", data, request.getRequestURI()));
        }
    }

    @Test
    void requestWithoutKeyIsRejectedWithStandard401Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/secure/whoami"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("API key is required"))
                .andExpect(jsonPath("$.error.code").value("API_KEY_REQUIRED"))
                .andExpect(jsonPath("$.path").value("/api/v1/secure/whoami"));
    }

    @Test
    void invalidKeyReturnsInvalidApiKeyCode() throws Exception {
        when(apiKeyService.authenticate(INVALID_KEY))
                .thenThrow(new ApiKeyAuthenticationException("Invalid API key", "INVALID_API_KEY"));

        mockMvc.perform(get("/api/v1/secure/whoami")
                        .header("X-API-KEY", INVALID_KEY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_API_KEY"));
    }

    @Test
    void inactiveKeyReturnsInactiveCode() throws Exception {
        when(apiKeyService.authenticate(anyString()))
                .thenThrow(new ApiKeyAuthenticationException("API key is inactive", "API_KEY_INACTIVE"));

        mockMvc.perform(get("/api/v1/secure/whoami").header("X-API-KEY", VALID_KEY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("API_KEY_INACTIVE"));
    }

    @Test
    void expiredKeyReturnsExpiredCode() throws Exception {
        when(apiKeyService.authenticate(anyString()))
                .thenThrow(new ApiKeyAuthenticationException("API key has expired", "API_KEY_EXPIRED"));

        mockMvc.perform(get("/api/v1/secure/whoami").header("X-API-KEY", VALID_KEY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("API_KEY_EXPIRED"));
    }

    @Test
    void validKeyAuthenticatesAndExposesOrganization() throws Exception {
        ApiKeyPrincipal principal = new ApiKeyPrincipal(42L, 10L, "prod-key");
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_API_KEY")));
        when(apiKeyService.authenticate(VALID_KEY)).thenReturn(authentication);

        mockMvc.perform(get("/api/v1/secure/whoami").header("X-API-KEY", VALID_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.organizationId").value(10))
                .andExpect(jsonPath("$.data.apiKeyId").value(42));
    }

    @Test
    void healthEndpointIsPermitAll() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(result ->
                        assertThat(result.getResponse().getStatus()).isNotEqualTo(401));
    }
}