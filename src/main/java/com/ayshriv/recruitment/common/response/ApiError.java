package com.ayshriv.recruitment.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Machine readable error details embedded in {@link ApiResponse}.
 *
 * <p>Uses an uppercase error code so API clients can react to failures
 * programmatically instead of parsing human readable messages. The
 * {@code details} payload carries contextual information, such as field
 * level validation errors, and must never expose internal stack traces
 * or sensitive information.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {

    /**
     * Machine readable error code, for example {@code CANDIDATE_NOT_FOUND}.
     */
    private String code;

    /**
     * Contextual error details, for example a map of field to error message.
     */
    private Object details;
}