package com.ayshriv.recruitment.user.mapper;

import com.ayshriv.recruitment.role.entity.Role;
import com.ayshriv.recruitment.user.dto.request.CreateUserRequest;
import com.ayshriv.recruitment.user.dto.request.UpdateUserRequest;
import com.ayshriv.recruitment.user.dto.response.RoleReference;
import com.ayshriv.recruitment.user.dto.response.UserResponse;
import com.ayshriv.recruitment.user.dto.response.UserSummaryResponse;
import com.ayshriv.recruitment.user.entity.User;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Converts between user request / response DTOs and the JPA entity.
 *
 * <p>The mapper never touches the password hash or the tenant: the password
 * is hashed by the service layer and the organization is resolved from the
 * security context. Response mapping never exposes the {@code passwordHash}
 * or the organization entity.</p>
 */
@Component
public class UserMapper {

    /**
     * Map an entity into its full response representation.
     *
     * @param user source entity
     * @return response DTO
     */
    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .profileImageUrl(user.getProfileImageUrl())
                .jobTitle(user.getJobTitle())
                .lastLoginAt(user.getLastLoginAt())
                .roles(toRoleReferences(user))
                .isActive(user.isActive())
                .createdOn(user.getCreatedOn())
                .updatedOn(user.getUpdatedOn())
                .build();
    }

    /**
     * Map an entity into its lightweight summary representation.
     *
     * @param user source entity
     * @return summary DTO
     */
    public UserSummaryResponse toSummaryResponse(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .jobTitle(user.getJobTitle())
                .isActive(user.isActive())
                .build();
    }

    /**
     * Build a transient entity from a creation request. Only profile fields
     * are mapped; password, organization and roles are set by the service.
     *
     * @param request creation payload
     * @return transient entity
     */
    public User toEntity(CreateUserRequest request) {
        User user = new User();
        applyProfileFields(user, request.getFirstName(), request.getLastName(), request.getEmail(),
                request.getPhone(), request.getProfileImageUrl(), request.getJobTitle());
        return user;
    }

    /**
     * Apply updatable profile fields from an update request onto an existing
     * entity. Immutable fields (id, tenant, timestamps, active, deleted) are
     * never touched.
     *
     * @param user    target entity
     * @param request update payload
     */
    public void updateEntity(User user, UpdateUserRequest request) {
        applyProfileFields(user, request.getFirstName(), request.getLastName(), request.getEmail(),
                request.getPhone(), request.getProfileImageUrl(), request.getJobTitle());
    }

    /**
     * Map the roles of a user into ordered lightweight references.
     *
     * @param user source entity
     * @return ordered role references
     */
    private List<RoleReference> toRoleReferences(User user) {
        return user.getRoles().stream()
                .sorted(Comparator.comparing(Role::getName))
                .map(role -> new RoleReference(role.getId(), role.getName()))
                .toList();
    }

    /**
     * Copy the shared profile fields onto an entity.
     */
    private void applyProfileFields(User user, String firstName, String lastName, String email,
                                    String phone, String profileImageUrl, String jobTitle) {
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setProfileImageUrl(profileImageUrl);
        user.setJobTitle(jobTitle);
    }
}