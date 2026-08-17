package com.ayshriv.recruitment.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Standard envelope for every REST API response in the application.
 *
 * <p>All controllers must return this structure so that API consumers
 * always receive a consistent contract, regardless of the feature or
 * whether the operation succeeded.</p>
 *
 * <p>Field layout:</p>
 * <ul>
 *     <li>{@code success} - whether the operation succeeded</li>
 *     <li>{@code message} - human readable message</li>
 *     <li>{@code data} - payload, {@code null} for errors and deletions</li>
 *     <li>{@code metadata} - pagination or contextual metadata</li>
 *     <li>{@code error} - machine readable error details, {@code null} on success</li>
 *     <li>{@code timestamp} - ISO-8601 server time of the response</li>
 *     <li>{@code path} - request path that produced the response</li>
 * </ul>
 *
 * @param <T> type of the payload
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /**
     * Whether the operation succeeded.
     */
    private boolean success;

    /**
     * Human readable message describing the outcome.
     */
    private String message;

    /**
     * Payload of the response.
     */
    private T data;

    /**
     * Pagination or contextual metadata attached to the response.
     */
    private Object metadata;

    /**
     * Machine readable error details, populated only for failed responses.
     */
    private ApiError error;

    /**
     * ISO-8601 server time when the response was produced.
     */
    private OffsetDateTime timestamp;

    /**
     * Request path that produced this response.
     */
    private String path;
}