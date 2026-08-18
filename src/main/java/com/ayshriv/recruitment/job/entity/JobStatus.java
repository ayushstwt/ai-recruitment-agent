package com.ayshriv.recruitment.job.entity;

/**
 * Lifecycle state of a job.
 *
 * <p>Stored as a string ({@code EnumType.STRING}) in the database so the
 * persisted value is stable and readable. Ordinal values are never stored.
 * Legal transitions are enforced by the job status transition service and
 * never by this enum.</p>
 */
public enum JobStatus {

    /**
     * Job is being drafted and has not been published yet.
     */
    DRAFT,

    /**
     * Job is live and accepting applications.
     */
    OPEN,

    /**
     * Job is temporarily paused but still expected to reopen.
     */
    ON_HOLD,

    /**
     * Job has been filled or withdrawn and remains for historical data.
     */
    CLOSED,

    /**
     * Job has been cancelled; it never completes the recruitment flow.
     */
    CANCELLED
}
