package com.ayshriv.recruitment.organization.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Organization representation exposed to API clients.
 *
 * <p>Never exposes the JPA entity or internal persistence details. In
 * particular, the linked {@code ApiKey} collection is never serialized,
 * preventing circular JSON relationships.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationResponse {

    private Long id;

    private String name;

    private String legalName;

    private String email;

    private String phone;

    private String website;

    private String description;

    private String industry;

    private String country;

    private String state;

    private String city;

    private String timezone;

    @JsonProperty("isActive")
    private boolean isActive;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "dd-MM-yyyy HH:mm:ss",
            timezone = "Asia/Kolkata"
    )
    private LocalDateTime createdOn;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "dd-MM-yyyy HH:mm:ss",
            timezone = "Asia/Kolkata"
    )
    private LocalDateTime updatedOn;
}
