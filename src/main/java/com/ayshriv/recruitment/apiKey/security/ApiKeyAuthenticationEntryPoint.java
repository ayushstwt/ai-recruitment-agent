package com.ayshriv.recruitment.apiKey.security;

import com.ayshriv.recruitment.common.response.ApiError;
import com.ayshriv.recruitment.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * Writes a standard {@code 401} {@link ApiResponse} envelope when a request
 * reaches the security chain without valid credentials.
 */
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final ZoneId TIMESTAMP_ZONE = ZoneId.of("Asia/Kolkata");

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String code = "API_KEY_REQUIRED";
        String message = "API key is required";

        if (authException instanceof ApiKeyAuthenticationException apiKeyException) {
            code = apiKeyException.getCode();
            message = apiKeyException.getMessage();
        }

        writeError(response, request, code, message);
    }

    /**
     * Serialize a {@code 401} error envelope. Used both by the entry point
     * and directly by the authentication filter after a failed lookup.
     *
     * @param response HTTP response
     * @param request  HTTP request
     * @param code     machine readable error code
     * @param message  human readable error message
     * @throws IOException when the body cannot be written
     */
    public void writeError(HttpServletResponse response, HttpServletRequest request,
                           String code, String message) throws IOException {
        ApiResponse<Object> body = ApiResponse.builder()
                .success(false)
                .message(message)
                .error(new ApiError(code, null))
                .timestamp(OffsetDateTime.now(TIMESTAMP_ZONE).truncatedTo(ChronoUnit.SECONDS))
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}