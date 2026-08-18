package com.ayshriv.recruitment.common.security;

import com.ayshriv.recruitment.apiKey.security.ApiKeyPrincipal;
import com.ayshriv.recruitment.common.exception.UnauthorizedException;
import com.ayshriv.recruitment.user.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * Read access to the current authenticated identity.
 *
 * <p>Today the only supported identity is an {@link ApiKeyPrincipal}, which
 * identifies the calling organization. The {@code getCurrentUserId()} and
 * {@code getCurrentRoles()} accessors prepare the architecture for a future
 * user session / JWT authentication flow backed by {@link UserPrincipal};
 * while only API key authentication exists they resolve to
 * {@link Optional#empty()}.</p>
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
     * User id of the current request, when user authentication is active.
     *
     * <p>Resolves only for {@link UserPrincipal} based authentication. With
     * API key authentication there is no acting user yet, so the result is
     * empty.</p>
     *
     * @return present user id when the request is authenticated as a user
     */
    public Optional<Long> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal.userId());
        }
        return Optional.empty();
    }

    /**
     * Role names of the current request, when user authentication is active.
     *
     * <p>Resolves only for {@link UserPrincipal} based authentication. With
     * API key authentication there are no user roles yet, so the result is
     * empty.</p>
     *
     * @return present set of role names when the request is authenticated as a user
     */
    public Optional<Set<String>> getCurrentRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return Optional.of(principal.roles());
        }
        return Optional.empty();
    }

    /**
     * Whether the current request is authenticated.
     *
     * @return {@code true} when authenticated
     */
    public boolean isAuthenticated() {
        return getCurrentPrincipal().isPresent() || getCurrentUserId().isPresent();
    }
}