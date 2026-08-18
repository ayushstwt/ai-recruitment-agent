package com.ayshriv.recruitment.job.dto.request;

import com.ayshriv.recruitment.job.entity.JobStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for changing the status of a job through the generic status
 * endpoint.
 *
 * <p>Only the target status is supplied; the transition is validated against
 * the allowed lifecycle rules by the status transition service.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateJobStatusRequest {

    /**
     * Target status to move the job to.
     */
    @NotNull(message = "Status is required")
    private JobStatus status;
}
