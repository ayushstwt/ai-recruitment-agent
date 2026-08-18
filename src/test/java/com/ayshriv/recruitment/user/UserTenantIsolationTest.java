package com.ayshriv.recruitment.user;

import com.ayshriv.recruitment.apiKey.security.ApiKeyAuthenticationEntryPoint;
import com.ayshriv.recruitment.apiKey.security.ApiKeyPrincipal;
import com.ayshriv.recruitment.apiKey.service.ApiKeyService;
import com.ayshriv.recruitment.common.config.AppProperties;
import com.ayshriv.recruitment.common.security.PasswordPolicyValidator;
import com.ayshriv.recruitment.common.security.SecurityConfig;
import com.ayshriv.recruitment.common.security.SecurityContextService;
import com.ayshriv.recruitment.organization.entity.Organization;
import com.ayshriv.recruitment.role.mapper.RoleMapper;
import com.ayshriv.recruitment.role.repository.RoleRepository;
import com.ayshriv.recruitment.role.service.RoleService;
import com.ayshriv.recruitment.user.controller.UserController;
import com.ayshriv.recruitment.user.entity.User;
import com.ayshriv.recruitment.user.mapper.UserMapper;
import com.ayshriv.recruitment.user.repository.UserRepository;
import com.ayshriv.recruitment.user.service.UserService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mandatory tenant isolation test for users.
 *
 * <p>Organization A and Organization B exist. User A belongs to organization A
 * and user B belongs to organization B. API key A authenticates as
 * organization A. Every attempt to reach user B through an id based endpoint
 * must fail with {@code 403 FORBIDDEN} and the {@code USER_ACCESS_DENIED}
 * error code.</p>
 *
 * <p>The real {@link UserService} and {@link RoleService} are wired into the
 * web slice so the isolation decision is produced by the actual service logic,
 * not by a mock.</p>
 */
@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, ApiKeyAuthenticationEntryPoint.class, SecurityContextService.class,
        AppProperties.class, PasswordPolicyValidator.class,
        UserService.class, UserMapper.class, RoleService.class, RoleMapper.class})
class UserTenantIsolationTest {

    private static final String API_KEY_A = "test-api-key-org-a";

    private static final String UPDATE_BODY = """
            {
                "firstName": "Jane",
                "lastName": "Smith",
                "email": "jane@example.com",
                "phone": "+910987654321",
                "jobTitle": "Recruitment Manager"
            }
            """;

    private static final String ASSIGN_ROLES_BODY = """
            {
                "roleIds": [1]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @Test
    void organizationACannotGetUserB() throws Exception {
        mockAuthentication();
        mockUserB();

        mockMvc.perform(get("/api/v1/users/2").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("USER_ACCESS_DENIED"));
    }

    @Test
    void organizationACannotUpdateUserB() throws Exception {
        mockAuthentication();
        mockUserB();

        mockMvc.perform(put("/api/v1/users/2")
                        .header("X-API-KEY", API_KEY_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("USER_ACCESS_DENIED"));
    }

    @Test
    void organizationACannotDeleteUserB() throws Exception {
        mockAuthentication();
        mockUserB();

        mockMvc.perform(delete("/api/v1/users/2").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("USER_ACCESS_DENIED"));
    }

    @Test
    void organizationACannotAssignRolesToUserB() throws Exception {
        mockAuthentication();
        mockUserB();

        mockMvc.perform(post("/api/v1/users/2/roles")
                        .header("X-API-KEY", API_KEY_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ASSIGN_ROLES_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("USER_ACCESS_DENIED"));
    }

    @Test
    void organizationACanAccessItsOwnUser() throws Exception {
        mockAuthentication();
        when(userRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(user(1L, 1L)));

        mockMvc.perform(get("/api/v1/users/1").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.email").value("userA@orga.com"));
    }

    @Test
    void missingUserReturns404() throws Exception {
        mockAuthentication();
        when(userRepository.findByIdAndNotDeleted(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/users/99").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }

    private void mockAuthentication() {
        when(apiKeyService.authenticate(API_KEY_A)).thenReturn(new UsernamePasswordAuthenticationToken(
                new ApiKeyPrincipal(1L, 1L, "org-a-key"), null,
                List.of(new SimpleGrantedAuthority("ROLE_API_KEY"))));
    }

    private void mockUserB() {
        when(userRepository.findByIdAndNotDeleted(2L)).thenReturn(Optional.of(user(2L, 2L)));
    }

    private User user(Long id, Long organizationId) {
        User user = new User();
        user.setId(id);
        user.setFirstName("User");
        user.setLastName(organizationId == 1L ? "A" : "B");
        user.setEmail(organizationId == 1L ? "userA@orga.com" : "userB@orgb.com");
        user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz1234567890");
        user.setJobTitle("Recruiter");
        user.setOrganization(new Organization(organizationId));
        user.setActive(true);
        user.setDeleted(false);
        user.setCreatedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
        user.setUpdatedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
        return user;
    }
}