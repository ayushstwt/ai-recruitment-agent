package com.ayshriv.recruitment.job.entity;

/**
 * Seniority band expected of a job candidate.
 *
 * <p>Stored as a string ({@code EnumType.STRING}) in the database so the
 * persisted value is stable and readable. Ordinal values are never stored.</p>
 */
public enum ExperienceLevel {

    /**
     * Entry level.
     */
    ENTRY,

    /**
     * Junior level.
     */
    JUNIOR,

    /**
     * Mid level.
     */
    MID,

    /**
     * Senior level.
     */
    SENIOR,

    /**
     * Lead level.
     */
    LEAD,

    /**
     * Manager level.
     */
    MANAGER,

    /**
     * Director level.
     */
    DIRECTOR
}
