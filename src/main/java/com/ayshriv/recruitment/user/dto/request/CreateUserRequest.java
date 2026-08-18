package com.ayshriv.recruitment.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Request payload for creating a user.
 *
 * <p>The tenant is never part of the payload: the organization is resolved
 * from the authenticated API key via the security context. The plaintext
 * password is validated against the configured policy, hashed and stored as
 * {@code passwordHash}; it is never persisted or returned.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    /**
     * First name of the user.
     */
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must be at most 100 characters")
    private String firstName;

    /**
     * Last name of the user.
     */
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must be at most 100 characters")
    private String lastName;

    /**
     * Login email, unique within the organization.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;

    /**
     * Contact phone number.
     */
    @Size(max = 30, message = "Phone must be at most 30 characters")
    @Pattern(regexp = "^\\+?[0-9()\\-\\s]{7,20}$", message = "Invalid phone number format")
    private String phone;

    /**
     * Temporary plaintext password, hashed before storage.
     */
    @NotBlank(message = "Password is required")
    @Size(max = 72, message = "Password must be at most 72 characters")
    private String password;

    /**
     * Profile picture URL.
     */
    @Size(max = 500, message = "Profile image URL must be at most 500 characters")
    private String profileImageUrl;

    /**
     * Display job title.
     */
    @Size(max = 100, message = "Job title must be at most 100 characters")
    private String jobTitle;

    /**
     * Role ids to assign at creation. Optional; may be empty.
     */
    private List<Long> roleIds;
}