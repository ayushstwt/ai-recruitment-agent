package com.ayshriv.recruitment.job.entity;

/**
 * Urgency band of a job.
 *
 * <p>Stored as a string ({@code EnumType.STRING}) in the database so the
 * persisted value is stable and readable. Ordinal values are never stored.</p>
 */
public enum JobPriority {

    /**
     * Low urgency.
     */
    LOW,

    /**
     * Normal urgency.
     */
    MEDIUM,

    /**
     * High urgency.
     */
    HIGH,

    /**
     * Must be filled as soon as possible.
     */
    URGENT
}
