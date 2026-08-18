package com.ayshriv.recruitment.job.service;

import com.ayshriv.recruitment.common.exception.BadRequestException;
import com.ayshriv.recruitment.job.entity.Job;
import com.ayshriv.recruitment.job.entity.JobStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Enforces the job lifecycle state machine.
 *
 * <p>Every status change goes through this service. Any transition that is not
 * explicitly allowed by the lifecycle is rejected with {@code 400 BAD_REQUEST}
 * so that invalid transitions (for example re-publishing a closed job or
 * closing a draft) fail fast and never reach the database.</p>
 *
 * <p>Allowed transitions:</p>
 * <ul>
 *     <li>{@code DRAFT → OPEN, CANCELLED}</li>
 *     <li>{@code OPEN → ON_HOLD, CLOSED, CANCELLED}</li>
 *     <li>{@code ON_HOLD → OPEN, CLOSED, CANCELLED}</li>
 *     <li>{@code CLOSED → } (terminal)</li>
 *     <li>{@code CANCELLED → } (terminal)</li>
 * </ul>
 */
@Component
public class JobStatusTransitionService {

    /**
     * Machine readable code returned when a status transition is invalid.
     */
    public static final String INVALID_STATUS_TRANSITION = "INVALID_STATUS_TRANSITION";

    private final Map<JobStatus, Set<JobStatus>> allowedTransitions =
            new EnumMap<>(JobStatus.class);

    /**
     * Build the transition table once.
     */
    public JobStatusTransitionService() {
        allowedTransitions.put(JobStatus.DRAFT, setOf(JobStatus.OPEN, JobStatus.CANCELLED));
        allowedTransitions.put(JobStatus.OPEN, setOf(JobStatus.ON_HOLD, JobStatus.CLOSED, JobStatus.CANCELLED));
        allowedTransitions.put(JobStatus.ON_HOLD, setOf(JobStatus.OPEN, JobStatus.CLOSED, JobStatus.CANCELLED));
        allowedTransitions.put(JobStatus.CLOSED, Set.of());
        allowedTransitions.put(JobStatus.CANCELLED, Set.of());
    }

    /**
     * Validate and apply a status change to a job.
     *
     * <p>Applies the current timestamp as {@code publishedOn} when the job is
     * first published (moving to {@code OPEN}). The {@code isActive} flag
     * follows the lifecycle: a job becomes inactive when closed or cancelled
     * and stays active while published or on hold.</p>
     *
     * @param job    target job
     * @param target desired status
     * @throws BadRequestException when the transition is not allowed
     */
    public void apply(Job job, JobStatus target) {
        JobStatus current = job.getStatus();
        if (!isAllowed(current, target)) {
            throw new BadRequestException(
                    "Invalid status transition from " + current + " to " + target,
                    INVALID_STATUS_TRANSITION);
        }
        job.setStatus(target);
        switch (target) {
            case OPEN -> job.setPublishedOn(LocalDateTime.now());
            case CLOSED, CANCELLED -> job.setActive(false);
            default -> {
            }
        }
    }

    /**
     * Whether a transition is allowed by the lifecycle rules.
     *
     * @param current current status
     * @param target  desired status
     * @return {@code true} when the transition is allowed
     */
    private boolean isAllowed(JobStatus current, JobStatus target) {
        return allowedTransitions.getOrDefault(current, Set.of()).contains(target);
    }

    /**
     * Build an immutable set of enum values.
     */
    @SafeVarargs
    private static <T extends Enum<T>> Set<T> setOf(T... values) {
        Set<T> set = new HashSet<>();
        java.util.Collections.addAll(set, values);
        return set;
    }
}
