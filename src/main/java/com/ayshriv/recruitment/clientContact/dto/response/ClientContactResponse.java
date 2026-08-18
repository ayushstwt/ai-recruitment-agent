package com.ayshriv.recruitment.clientContact.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Client contact representation exposed to API clients.
 *
 * <p>Never exposes the JPA entity, the owning client entity or the owning
 * organization entity.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientContactResponse {

    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private String phone;

    private String jobTitle;

    private String department;

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