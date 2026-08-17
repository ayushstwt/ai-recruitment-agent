package com.ayshriv.recruitment.apiKey.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * API key representation for regular responses.
 *
 * <p>Never contains the raw key value. The {@code keyPrefix} is masked so
 * only the identifying portion of the key is visible.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyResponse {

    private Long id;
    private String name;
    private String keyPrefix;
    private String description;
    private LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private Long organizationId;
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;
    private boolean isActive;
}