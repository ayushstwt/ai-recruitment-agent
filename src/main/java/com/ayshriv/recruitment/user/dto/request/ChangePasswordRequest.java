package com.ayshriv.recruitment.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for changing a user's password.
 *
 * <p>The current password is verified before the new one is accepted, and the
 * new password is validated against the configured policy before it is hashed.
 * Password values are never returned in any response.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    /**
     * Current password, verified against the stored hash.
     */
    @NotBlank(message = "Current password is required")
    @Size(max = 72, message = "Current password must be at most 72 characters")
    private String currentPassword;

    /**
     * New password, validated and hashed before storage.
     */
    @NotBlank(message = "New password is required")
    @Size(max = 72, message = "New password must be at most 72 characters")
    private String newPassword;
}