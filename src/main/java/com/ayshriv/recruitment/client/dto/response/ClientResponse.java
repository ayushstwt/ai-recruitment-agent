package com.ayshriv.recruitment.client.dto.response;

import com.ayshriv.recruitment.client.entity.CompanySize;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Full client representation exposed to API clients.
 *
 * <p>Never exposes the JPA entity, the owning organization entity or the
 * contacts collection. Contacts are always handled through dedicated contact
 * endpoints.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientResponse {

    private Long id;

    private String clientCode;

    private String companyName;

    private String legalName;

    private String email;

    private String phone;

    private String website;

    private String industry;

    private CompanySize companySize;

    private String country;

    private String state;

    private String city;

    private String address;

    private String postalCode;

    private String timezone;

    private String description;

    private String notes;

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