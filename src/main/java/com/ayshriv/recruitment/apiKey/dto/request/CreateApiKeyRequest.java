package com.ayshriv.recruitment.apiKey.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Request payload for creating a new API key.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateApiKeyRequest {

    /**
     * Display name of the key.
     */
    @NotBlank(message = "Key name is required")
    @Size(max = 100, message = "Key name must be at most 100 characters")
    private String name;

    /**
     * Optional description of the key's purpose.
     */
    @Size(max = 500, message = "Description must be at most 500 characters")
    private String description;

    /**
     * Optional expiry timestamp; {@code null} means the key never expires.
     */
    private LocalDateTime expiresAt;
}