package com.ayshriv.recruitment.user.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full user representation exposed to API clients.
 *
 * <p>Never exposes the JPA entity, the {@code passwordHash}, the raw password
 * or the owning organization entity. Roles are serialized only as lightweight
 * {@link RoleReference} objects.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private String phone;

    private String profileImageUrl;

    private String jobTitle;

    private LocalDateTime lastLoginAt;

    private List<RoleReference> roles;

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