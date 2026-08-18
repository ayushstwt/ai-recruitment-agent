package com.ayshriv.recruitment.clientContact.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for creating a client contact.
 *
 * <p>The client and the tenant are never part of the payload: the client is
 * addressed by the URL and the organization is resolved from the
 * authenticated API key via the security context.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateClientContactRequest {

    /**
     * First name of the contact.
     */
    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must be at most 100 characters")
    private String firstName;

    /**
     * Last name of the contact.
     */
    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must be at most 100 characters")
    private String lastName;

    /**
     * Contact email.
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
     * Job title of the contact.
     */
    @Size(max = 100, message = "Job title must be at most 100 characters")
    private String jobTitle;

    /**
     * Department of the contact.
     */
    @Size(max = 100, message = "Department must be at most 100 characters")
    private String department;

    /**
     * Internal recruitment notes about the contact.
     */
    @Size(max = 2000, message = "Notes must be at most 2000 characters")
    private String notes;
}