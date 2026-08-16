package com.ayshriv.recruitment.apiKey.security;

/**
 * Authenticated identity attached to a request authenticated with an API key.
 *
 * @param apiKeyId       primary key of the matching {@code ApiKey}
 * @param organizationId owning tenant
 * @param keyName        display name of the key
 */
public record ApiKeyPrincipal(Long apiKeyId, Long organizationId, String keyName) {
}