package com.ayshriv.recruitment.organization.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for updating an organization.
 *
 * <p>Only business profile fields are updatable. The client must never be
 * able to change the primary key, lifecycle timestamps, {@code isActive} or
 * {@code isDeleted}; those remain under the control of the service.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrganizationRequest {

    /**
     * Human readable organization name.
     */
    @NotBlank(message = "Organization name is required")
    @Size(max = 100, message = "Organization name must be at most 100 characters")
    private String name;

    /**
     * Registered / legal name of the organization.
     */
    @Size(max = 200, message = "Legal name must be at most 200 characters")
    private String legalName;

    /**
     * Primary contact email.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    @Size(max = 255, message = "Email must be at most 255 characters")
    private String email;

    /**
     * Primary contact phone number.
     */
    @Size(max = 30, message = "Phone must be at most 30 characters")
    @Pattern(regexp = "^\\+?[0-9()\\-\\s]{7,20}$", message = "Invalid phone number format")
    private String phone;

    /**
     * Organization website URL.
     */
    @Size(max = 255, message = "Website must be at most 255 characters")
    @Pattern(regexp = "^(|(https?://)?[\\w-]+(\\.[\\w-]+)+[\\w./@?=~_-]*)$", message = "Invalid website URL")
    private String website;

    /**
     * Short description of the organization.
     */
    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;

    /**
     * Industry sector.
     */
    @Size(max = 100, message = "Industry must be at most 100 characters")
    private String industry;

    /**
     * Country where the organization operates.
     */
    @Size(max = 100, message = "Country must be at most 100 characters")
    private String country;

    /**
     * State / province where the organization operates.
     */
    @Size(max = 100, message = "State must be at most 100 characters")
    private String state;

    /**
     * City where the organization operates.
     */
    @Size(max = 100, message = "City must be at most 100 characters")
    private String city;

    /**
     * IANA timezone of the organization.
     */
    @Size(max = 50, message = "Timezone must be at most 50 characters")
    private String timezone;
}
