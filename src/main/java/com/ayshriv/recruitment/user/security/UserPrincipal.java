package com.ayshriv.recruitment.user.security;

import java.util.Set;

/**
 * Authenticated user identity for a future user session / JWT flow.
 *
 * <p>Not active yet: authentication currently relies on
 * {@link com.ayshriv.recruitment.apiKey.security.ApiKeyPrincipal}. This
 * record establishes the shape the security layer will expose once user
 * authentication lands, so {@code SecurityContextService} can resolve
 * {@code getCurrentUserId()} and {@code getCurrentRoles()} without
 * breaking the existing API key flow.</p>
 *
 * @param userId         primary key of the acting {@code User}
 * @param organizationId owning tenant of the user
 * @param email          email of the user
 * @param roles          normalized role names of the user
 */
public record UserPrincipal(Long userId, Long organizationId, String email, Set<String> roles) {
}