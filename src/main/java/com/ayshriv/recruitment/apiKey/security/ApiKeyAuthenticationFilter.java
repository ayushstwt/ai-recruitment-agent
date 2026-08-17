package com.ayshriv.recruitment.apiKey.security;

import com.ayshriv.recruitment.apiKey.service.ApiKeyService;
import com.ayshriv.recruitment.common.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Extracts the API key from the configured header and authenticates the
 * request when present. Requests without a header pass through and are
 * rejected later by the authorization layer.
 *
 * <p>Registered as a bean by {@code SecurityConfig}; not a component so
 * web layer slices remain independent of authentication dependencies.</p>
 */
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;
    private final ApiKeyAuthenticationEntryPoint entryPoint;
    private final AppProperties appProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawKey = request.getHeader(appProperties.getSecurity().getApiKey().getHeaderName());
        if (rawKey == null || rawKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Authentication authentication = apiKeyService.authenticate(rawKey);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            ApiKeyPrincipal principal = (ApiKeyPrincipal) authentication.getPrincipal();
            apiKeyService.markKeyUsed(principal.apiKeyId());
        } catch (ApiKeyAuthenticationException e) {
            SecurityContextHolder.clearContext();
            entryPoint.writeError(response, request, e.getCode(), e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }
}