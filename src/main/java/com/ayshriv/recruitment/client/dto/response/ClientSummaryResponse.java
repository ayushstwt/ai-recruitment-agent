package com.ayshriv.recruitment.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lightweight client representation used for dropdowns, job creation,
 * application views and search results.
 *
 * <p>Carries the essential identity fields without the full profile, keeping
 * collection payloads small. Full detail is available through the single
 * client endpoint.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientSummaryResponse {

    private Long id;

    private String clientCode;

    private String companyName;

    private String industry;

    @JsonProperty("isActive")
    private boolean isActive;
}