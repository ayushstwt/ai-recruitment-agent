package com.ayshriv.recruitment.apiKey.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Response returned only when a key is created.
 *
 * <p>Contains the raw {@code key} value which is shown to the client
 * exactly once and never persisted.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyCreatedResponse {

    private Long id;
    private String name;
    private String key;
    private String keyPrefix;
    private String description;
    private LocalDateTime expiresAt;
    private Long organizationId;
    private LocalDateTime createdOn;
}