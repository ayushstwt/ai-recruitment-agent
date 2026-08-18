package com.ayshriv.recruitment.job.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for reassigning a job to another client.
 *
 * <p>Only the target client id is supplied; the service verifies that the new
 * client belongs to the same organization as the job before reassigning.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeJobClientRequest {

    /**
     * Target client company, verified to belong to the authenticated organization.
     */
    @NotNull(message = "Client is required")
    private Long clientId;
}
