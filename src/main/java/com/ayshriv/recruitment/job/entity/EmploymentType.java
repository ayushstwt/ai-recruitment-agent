package com.ayshriv.recruitment.job.entity;

/**
 * Contract arrangement of a job.
 *
 * <p>Stored as a string ({@code EnumType.STRING}) in the database so the
 * persisted value is stable and readable. Ordinal values are never stored.</p>
 */
public enum EmploymentType {

    /**
     * Full time employment.
     */
    FULL_TIME,

    /**
     * Part time employment.
     */
    PART_TIME,

    /**
     * Fixed term contract.
     */
    CONTRACT,

    /**
     * Contract with a path to a full time hire.
     */
    CONTRACT_TO_HIRE,

    /**
     * Internship.
     */
    INTERNSHIP,

    /**
     * Temporary assignment.
     */
    TEMPORARY
}
