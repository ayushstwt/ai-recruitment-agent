package com.ayshriv.recruitment.common.response;

import org.springframework.data.domain.Page;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Factory for building consistent {@link ApiResponse} envelopes.
 *
 * <p>Controllers must use these helpers instead of constructing
 * {@link ApiResponse} instances manually. This class contains only
 * response construction and no business logic.</p>
 */
public final class ResponseUtil {

    /**
     * Timezone used for response timestamps.
     */
    private static final ZoneId TIMESTAMP_ZONE = ZoneId.of("Asia/Kolkata");

    private ResponseUtil() {
    }

    /**
     * Current time in {@link #TIMESTAMP_ZONE} truncated to whole seconds.
     *
     * @return response timestamp
     */
    private static OffsetDateTime now() {
        return OffsetDateTime.now(TIMESTAMP_ZONE).truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * Build a successful response without metadata.
     *
     * @param message success message
     * @param data    payload
     * @param path    request path
     * @param <T>     type of the payload
     * @return successful response
     */
    public static <T> ApiResponse<T> success(String message, T data, String path) {
        return success(message, data, null, path);
    }

    /**
     * Build a successful response with metadata.
     *
     * @param message  success message
     * @param data     payload
     * @param metadata pagination or contextual metadata, may be {@code null}
     * @param path     request path
     * @param <T>      type of the payload
     * @return successful response
     */
    public static <T> ApiResponse<T> success(String message, T data, Object metadata, String path) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .metadata(metadata)
                .timestamp(now())
                .path(path)
                .build();
    }

    /**
     * Build a failed response.
     *
     * @param message   human readable error message
     * @param errorCode machine readable error code
     * @param details   contextual error details, may be {@code null}
     * @param path      request path
     * @return failed response
     */
    public static ApiResponse<Object> error(String message, String errorCode, Object details, String path) {
        return ApiResponse.builder()
                .success(false)
                .message(message)
                .error(new ApiError(errorCode, details))
                .timestamp(now())
                .path(path)
                .build();
    }

    /**
     * Build a successful paginated response from a Spring Data page.
     *
     * <p>The page content becomes the {@code data} list and the page
     * details are exposed as {@link PageMetadata}.</p>
     *
     * @param message success message
     * @param page    Spring Data page
     * @param path    request path
     * @param <T>     type of the items in the page
     * @return successful paginated response
     */
    public static <T> ApiResponse<List<T>> successPage(String message, Page<T> page, String path) {
        return success(message, page.getContent(), PageMetadata.of(page), path);
    }
}