package com.ayshriv.recruitment.user.controller;

import com.ayshriv.recruitment.apiKey.security.ApiKeyAuthenticationEntryPoint;
import com.ayshriv.recruitment.apiKey.security.ApiKeyPrincipal;
import com.ayshriv.recruitment.apiKey.service.ApiKeyService;
import com.ayshriv.recruitment.common.config.AppProperties;
import com.ayshriv.recruitment.common.exception.BadRequestException;
import com.ayshriv.recruitment.common.exception.ForbiddenException;
import com.ayshriv.recruitment.common.security.PasswordPolicyValidator;
import com.ayshriv.recruitment.common.security.SecurityConfig;
import com.ayshriv.recruitment.common.security.SecurityContextService;
import com.ayshriv.recruitment.role.dto.response.RoleResponse;
import com.ayshriv.recruitment.user.dto.response.RoleReference;
import com.ayshriv.recruitment.user.dto.response.UserResponse;
import com.ayshriv.recruitment.user.dto.response.UserSummaryResponse;
import com.ayshriv.recruitment.user.exception.UserNotFoundException;
import com.ayshriv.recruitment.user.service.UserService;
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

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, ApiKeyAuthenticationEntryPoint.class, SecurityContextService.class,
        AppProperties.class})
class UserControllerTest {

    private static final String API_KEY = "test-api-key";

    private static final String CREATE_BODY = """
            {
                "firstName": "John",
                "lastName": "Doe",
                "email": "john@example.com",
                "phone": "+911234567890",
                "password": "TemporaryPassword123!",
                "profileImageUrl": "https://example.com/john.png",
                "jobTitle": "Senior Recruiter",
                "roleIds": [1]
            }
            """;

    private static final String UPDATE_BODY = """
            {
                "firstName": "Jane",
                "lastName": "Smith",
                "email": "jane@example.com",
                "phone": "+910987654321",
                "profileImageUrl": "https://example.com/jane.png",
                "jobTitle": "Recruitment Manager"
            }
            """;

    private static final String ASSIGN_ROLES_BODY = """
            {
                "roleIds": [1, 3]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @BeforeEach
    void authenticate() {
        when(apiKeyService.authenticate(API_KEY)).thenReturn(new UsernamePasswordAuthenticationToken(
                new ApiKeyPrincipal(1L, 1L, "org-a-key"), null,
                List.of(new SimpleGrantedAuthority("ROLE_API_KEY"))));
    }

    private UserResponse userResponse() {
        return UserResponse.builder()
                .id(10L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phone("+911234567890")
                .profileImageUrl("https://example.com/john.png")
                .jobTitle("Senior Recruiter")
                .lastLoginAt(null)
                .roles(List.of(new RoleReference(1L, "AGENCY_ADMIN")))
                .isActive(true)
                .createdOn(LocalDateTime.of(2026, 1, 1, 10, 0))
                .updatedOn(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();
    }

    private UserSummaryResponse summaryResponse() {
        return UserSummaryResponse.builder()
                .id(10L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .jobTitle("Senior Recruiter")
                .isActive(true)
                .build();
    }

    @Test
    void createReturns201WithStandardEnvelope() throws Exception {
        when(userService.createUser(eq(1L), any())).thenReturn(userResponse());

        mockMvc.perform(post("/api/v1/users")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.roles[0].name").value("AGENCY_ADMIN"))
                .andExpect(jsonPath("$.data.isActive").value(true))
                .andExpect(jsonPath("$.metadata").isEmpty())
                .andExpect(jsonPath("$.error").isEmpty())
                .andExpect(jsonPath("$.path").value("/api/v1/users"));

        verify(userService).createUser(eq(1L), any());
    }

    @Test
    void createResponseNeverExposesPasswordOrHash() throws Exception {
        when(userService.createUser(eq(1L), any())).thenReturn(userResponse());

        mockMvc.perform(post("/api/v1/users")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.data.organization").doesNotExist());
    }

    @Test
    void createReturns400ForValidationErrors() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.firstName").value("First name is required"))
                .andExpect(jsonPath("$.error.details.email").value("Invalid email address"))
                .andExpect(jsonPath("$.error.details.password").value("Password is required"));
    }

    @Test
    void listReturnsPaginatedResponseWithMetadata() throws Exception {
        when(userService.getUsers(eq(1L), any())).thenReturn(
                new PageImpl<>(List.of(summaryResponse()), PageRequest.of(0, 20), 25));

        mockMvc.perform(get("/api/v1/users").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].email").value("john@example.com"))
                .andExpect(jsonPath("$.data[0].roles").doesNotExist())
                .andExpect(jsonPath("$.metadata.page").value(0))
                .andExpect(jsonPath("$.metadata.size").value(20))
                .andExpect(jsonPath("$.metadata.totalElements").value(25))
                .andExpect(jsonPath("$.metadata.totalPages").value(2))
                .andExpect(jsonPath("$.metadata.hasNext").value(true))
                .andExpect(jsonPath("$.metadata.hasPrevious").value(false));
    }

    @Test
    void searchReturnsPaginatedResults() throws Exception {
        when(userService.searchUsers(eq(1L), eq("john"), any())).thenReturn(
                new PageImpl<>(List.of(summaryResponse()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/users/search")
                        .param("keyword", "john")
                        .header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].jobTitle").value("Senior Recruiter"))
                .andExpect(jsonPath("$.metadata.totalElements").value(1));
    }

    @Test
    void getReturnsUserAndUsesAuthenticatedTenant() throws Exception {
        when(userService.getUserById(1L, 10L)).thenReturn(userResponse());

        mockMvc.perform(get("/api/v1/users/10").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(10));

        verify(userService).getUserById(1L, 10L);
    }

    @Test
    void getReturns404ForMissingUser() throws Exception {
        when(userService.getUserById(1L, 99L)).thenThrow(new UserNotFoundException(99L));

        mockMvc.perform(get("/api/v1/users/99").header("X-API-KEY", API_KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }

    @Test
    void getReturns403ForCrossTenantUser() throws Exception {
        when(userService.getUserById(1L, 2L))
                .thenThrow(new ForbiddenException("Access denied to the requested user",
                        UserService.USER_ACCESS_DENIED));

        mockMvc.perform(get("/api/v1/users/2").header("X-API-KEY", API_KEY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("USER_ACCESS_DENIED"));

        verify(userService).getUserById(1L, 2L);
    }

    @Test
    void updateReturnsUpdatedUser() throws Exception {
        when(userService.updateUser(eq(1L), eq(10L), any())).thenReturn(userResponse());

        mockMvc.perform(put("/api/v1/users/10")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User updated successfully"))
                .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    void updateReturns400ForValidationErrors() throws Exception {
        mockMvc.perform(put("/api/v1/users/10")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"email\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.firstName").value("First name is required"))
                .andExpect(jsonPath("$.error.details.email").value("Email is required"));
    }

    @Test
    void activateReturnsActivatedUser() throws Exception {
        when(userService.activateUser(1L, 10L)).thenReturn(userResponse());

        mockMvc.perform(patch("/api/v1/users/10/activate").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User activated successfully"))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    void deactivateReturnsDeactivatedUser() throws Exception {
        UserResponse inactive = userResponse();
        inactive.setActive(false);
        when(userService.deactivateUser(1L, 10L)).thenReturn(inactive);

        mockMvc.perform(patch("/api/v1/users/10/deactivate").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User deactivated successfully"))
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/users/10").header("X-API-KEY", API_KEY))
                .andExpect(status().isNoContent());
        verify(userService).deleteUser(1L, 10L);
    }

    @Test
    void deleteReturns403ForCrossTenantUser() throws Exception {
        doThrow(new ForbiddenException("Access denied to the requested user",
                UserService.USER_ACCESS_DENIED))
                .when(userService).deleteUser(1L, 2L);

        mockMvc.perform(delete("/api/v1/users/2").header("X-API-KEY", API_KEY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("USER_ACCESS_DENIED"));
    }

    @Test
    void changePasswordReturns200() throws Exception {
        mockMvc.perform(patch("/api/v1/users/10/password")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "currentPassword": "OldPassword123!",
                                    "newPassword": "NewPassword123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully"))
                .andExpect(jsonPath("$.data").isEmpty());
        verify(userService).changePassword(eq(1L), eq(10L), any());
    }

    @Test
    void changePasswordReturns400ForWrongCurrentPassword() throws Exception {
        doThrow(new BadRequestException("Current password is incorrect",
                PasswordPolicyValidator.INVALID_PASSWORD))
                .when(userService).changePassword(eq(1L), eq(10L), any());

        mockMvc.perform(patch("/api/v1/users/10/password")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "currentPassword": "WrongPassword123!",
                                    "newPassword": "NewPassword123!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PASSWORD"));
    }

    @Test
    void changePasswordReturns400ForBlankFields() throws Exception {
        mockMvc.perform(patch("/api/v1/users/10/password")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"\",\"newPassword\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void assignRolesReturnsUpdatedUser() throws Exception {
        when(userService.assignRoles(eq(1L), eq(10L), any())).thenReturn(userResponse());

        mockMvc.perform(post("/api/v1/users/10/roles")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ASSIGN_ROLES_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Roles assigned successfully"))
                .andExpect(jsonPath("$.data.roles[0].name").value("AGENCY_ADMIN"));
    }

    @Test
    void assignRolesReturns400ForMissingRoleIds() throws Exception {
        mockMvc.perform(post("/api/v1/users/10/roles")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.roleIds").value("At least one role id is required"));
    }

    @Test
    void getRolesReturnsRoleList() throws Exception {
        when(userService.getUserRoles(1L, 10L)).thenReturn(
                List.of(RoleResponse.builder().id(1L).name("AGENCY_ADMIN")
                        .description("Full access").isSystemRole(true).build()));

        mockMvc.perform(get("/api/v1/users/10/roles").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Roles retrieved successfully"))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("AGENCY_ADMIN"))
                .andExpect(jsonPath("$.data[0].isSystemRole").value(true));
    }

    @Test
    void removeRoleReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/users/10/roles/3").header("X-API-KEY", API_KEY))
                .andExpect(status().isNoContent());
        verify(userService).removeRole(1L, 10L, 3L);
    }

    @Test
    void unauthenticatedProtectedRequestReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/10"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("API_KEY_REQUIRED"));
    }
}