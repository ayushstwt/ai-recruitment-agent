package com.ayshriv.recruitment.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lightweight user representation used for list and search results.
 *
 * <p>Carries the essential identity and profile fields without the role
 * collection, keeping collection payloads small. Full detail is available
 * through the single-user endpoints.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryResponse {

    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private String jobTitle;

    @JsonProperty("isActive")
    private boolean isActive;
}