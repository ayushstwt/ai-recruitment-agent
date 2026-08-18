package com.ayshriv.recruitment.client.entity;

/**
 * Size band of a client company.
 *
 * <p>Stored as a string ({@code EnumType.STRING}) in the database so the
 * persisted value is stable and readable. Ordinal values are never stored.</p>
 */
public enum CompanySize {

    /**
     * Early stage company.
     */
    STARTUP,

    /**
     * Small company.
     */
    SMALL,

    /**
     * Medium company.
     */
    MEDIUM,

    /**
     * Large company.
     */
    LARGE,

    /**
     * Large scale enterprise.
     */
    ENTERPRISE
}