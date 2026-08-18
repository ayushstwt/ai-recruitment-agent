package com.ayshriv.recruitment.user.mapper;

import com.ayshriv.recruitment.organization.entity.Organization;
import com.ayshriv.recruitment.role.entity.Role;
import com.ayshriv.recruitment.user.dto.request.CreateUserRequest;
import com.ayshriv.recruitment.user.dto.request.UpdateUserRequest;
import com.ayshriv.recruitment.user.dto.response.RoleReference;
import com.ayshriv.recruitment.user.dto.response.UserResponse;
import com.ayshriv.recruitment.user.dto.response.UserSummaryResponse;
import com.ayshriv.recruitment.user.entity.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    private User user() {
        Role recruiter = role(3L, "RECRUITER");
        Role admin = role(1L, "AGENCY_ADMIN");

        Set<Role> roles = new LinkedHashSet<>(List.of(recruiter, admin));

        User user = new User();
        user.setId(10L);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail("john@example.com");
        user.setPhone("+911234567890");
        user.setPasswordHash("$2a$10$secretHashValue");
        user.setProfileImageUrl("https://example.com/john.png");
        user.setJobTitle("Senior Recruiter");
        user.setLastLoginAt(LocalDateTime.of(2026, 2, 1, 9, 30));
        user.setOrganization(new Organization(5L));
        user.setRoles(roles);
        user.setActive(true);
        user.setDeleted(false);
        user.setCreatedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
        user.setUpdatedOn(LocalDateTime.of(2026, 1, 2, 10, 0));
        return user;
    }

    private Role role(Long id, String name) {
        Role role = new Role();
        role.setId(id);
        role.setName(name);
        return role;
    }

    @Test
    void toResponseMapsEverySafeField() {
        UserResponse response = mapper.toResponse(user());

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getPhone()).isEqualTo("+911234567890");
        assertThat(response.getProfileImageUrl()).isEqualTo("https://example.com/john.png");
        assertThat(response.getJobTitle()).isEqualTo("Senior Recruiter");
        assertThat(response.getLastLoginAt()).isEqualTo(LocalDateTime.of(2026, 2, 1, 9, 30));
        assertThat(response.isActive()).isTrue();
        assertThat(response.getCreatedOn()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0));
        assertThat(response.getUpdatedOn()).isEqualTo(LocalDateTime.of(2026, 1, 2, 10, 0));
    }

    @Test
    void toResponseNeverExposesPasswordHashOrOrganization() {
        UserResponse response = mapper.toResponse(user());

        assertThat(response).hasFieldOrProperty("email");
        assertThat(hasField(response, "passwordHash")).isFalse();
        assertThat(hasField(response, "organization")).isFalse();
    }

    @Test
    void toResponseMapsRolesAsOrderedReferences() {
        UserResponse response = mapper.toResponse(user());

        assertThat(response.getRoles()).hasSize(2);
        assertThat(response.getRoles().stream().map(RoleReference::getName).toList())
                .containsExactly("AGENCY_ADMIN", "RECRUITER");
        assertThat(response.getRoles().get(0).getId()).isEqualTo(1L);
    }

    @Test
    void toSummaryResponseMapsEssentialFieldsOnly() {
        UserSummaryResponse response = mapper.toSummaryResponse(user());

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getJobTitle()).isEqualTo("Senior Recruiter");
        assertThat(response.isActive()).isTrue();
        assertThat(hasField(response, "roles")).isFalse();
    }

    @Test
    void toEntityMapsCreationRequestWithoutSecrets() {
        CreateUserRequest request = new CreateUserRequest(
                "John", "Doe", "john@example.com", "+911234567890",
                "TemporaryPassword123!", "https://example.com/john.png", "Senior Recruiter",
                List.of(1L, 3L));

        User user = mapper.toEntity(request);

        assertThat(user.getId()).isNull();
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
        assertThat(user.getEmail()).isEqualTo("john@example.com");
        assertThat(user.getPhone()).isEqualTo("+911234567890");
        assertThat(user.getProfileImageUrl()).isEqualTo("https://example.com/john.png");
        assertThat(user.getJobTitle()).isEqualTo("Senior Recruiter");
        assertThat(user.getPasswordHash()).isNull();
        assertThat(user.getOrganization()).isNull();
        assertThat(user.getRoles()).isEmpty();
    }

    @Test
    void updateEntityChangesProfileFieldsAndLeavesLifecycleAlone() {
        User user = user();
        UpdateUserRequest request = new UpdateUserRequest(
                "Jane", "Smith", "jane@example.com", "+910987654321",
                "https://example.com/jane.png", "Recruitment Manager");

        mapper.updateEntity(user, request);

        assertThat(user.getFirstName()).isEqualTo("Jane");
        assertThat(user.getLastName()).isEqualTo("Smith");
        assertThat(user.getEmail()).isEqualTo("jane@example.com");
        assertThat(user.getPhone()).isEqualTo("+910987654321");
        assertThat(user.getProfileImageUrl()).isEqualTo("https://example.com/jane.png");
        assertThat(user.getJobTitle()).isEqualTo("Recruitment Manager");
        assertThat(user.getId()).isEqualTo(10L);
        assertThat(user.getCreatedOn()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0));
        assertThat(user.isActive()).isTrue();
        assertThat(user.isDeleted()).isFalse();
        assertThat(user.getOrganization().getId()).isEqualTo(5L);
    }

    private boolean hasField(Object object, String fieldName) {
        try {
            object.getClass().getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}