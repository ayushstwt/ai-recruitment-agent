package com.ayshriv.recruitment.client.dto.request;

import com.ayshriv.recruitment.client.entity.CompanySize;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for updating a client company.
 *
 * <p>Only business profile fields are updatable. The client must never be
 * able to change the primary key, the client code, the tenant, lifecycle
 * timestamps, {@code isActive} or {@code isDeleted}. Activation state is
 * changed through the dedicated activate / deactivate endpoints.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateClientRequest {

    /**
     * Display name of the client company.
     */
    @NotBlank(message = "Company name is required")
    @Size(max = 200, message = "Company name must be at most 200 characters")
    private String companyName;

    /**
     * Registered / legal name of the client company.
     */
    @Size(max = 255, message = "Legal name must be at most 255 characters")
    private String legalName;

    /**
     * Primary contact email, unique within the organization.
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
     * Client company website URL.
     */
    @Size(max = 255, message = "Website must be at most 255 characters")
    @Pattern(regexp = "^(|(https?://)?[\\w-]+(\\.[\\w-]+)+[\\w./@?=~_-]*)$", message = "Invalid website URL")
    private String website;

    /**
     * Industry sector.
     */
    @Size(max = 100, message = "Industry must be at most 100 characters")
    private String industry;

    /**
     * Company size band.
     */
    private CompanySize companySize;

    /**
     * Country where the client is located.
     */
    @Size(max = 100, message = "Country must be at most 100 characters")
    private String country;

    /**
     * State / province where the client is located.
     */
    @Size(max = 100, message = "State must be at most 100 characters")
    private String state;

    /**
     * City where the client is located.
     */
    @Size(max = 100, message = "City must be at most 100 characters")
    private String city;

    /**
     * Street / building address.
     */
    @Size(max = 500, message = "Address must be at most 500 characters")
    private String address;

    /**
     * Postal / zip code.
     */
    @Size(max = 20, message = "Postal code must be at most 20 characters")
    private String postalCode;

    /**
     * IANA timezone of the client.
     */
    @Size(max = 50, message = "Timezone must be at most 50 characters")
    private String timezone;

    /**
     * Short description of the client company.
     */
    @Size(max = 2000, message = "Description must be at most 2000 characters")
    private String description;

    /**
     * Internal recruitment notes about the client.
     */
    @Size(max = 2000, message = "Notes must be at most 2000 characters")
    private String notes;
}