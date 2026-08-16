package com.ayshriv.recruitment.common.security;

import com.ayshriv.recruitment.apiKey.security.ApiKeyPrincipal;
import com.ayshriv.recruitment.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Read access to the current authenticated {@link ApiKeyPrincipal}.
 */
@Component
public class SecurityContextService {

    /**
     * Resolve the current principal, if any.
     *
     * @return present principal when the request is authenticated
     */
    public Optional<ApiKeyPrincipal> getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof ApiKeyPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    /**
     * Owning tenant of the current request.
     *
     * @return organization id
     * @throws UnauthorizedException when the request is not authenticated
     */
    public Long getCurrentOrganizationId() {
        return getCurrentPrincipal()
                .map(ApiKeyPrincipal::organizationId)
                .orElseThrow(() -> new UnauthorizedException("Authentication required", "AUTHENTICATION_REQUIRED"));
    }

    /**
     * Key id used to authenticate the current request.
     *
     * @return api key id
     * @throws UnauthorizedException when the request is not authenticated
     */
    public Long getCurrentApiKeyId() {
        return getCurrentPrincipal()
                .map(ApiKeyPrincipal::apiKeyId)
                .orElseThrow(() -> new UnauthorizedException("Authentication required", "AUTHENTICATION_REQUIRED"));
    }

    /**
     * Whether the current request is authenticated.
     *
     * @return {@code true} when authenticated
     */
    public boolean isAuthenticated() {
        return getCurrentPrincipal().isPresent();
    }
}