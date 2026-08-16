package com.ayshriv.recruitment.apiKey.service;

import com.ayshriv.recruitment.apiKey.dto.request.CreateApiKeyRequest;
import com.ayshriv.recruitment.apiKey.dto.request.UpdateApiKeyRequest;
import com.ayshriv.recruitment.apiKey.dto.response.ApiKeyCreatedResponse;
import com.ayshriv.recruitment.apiKey.dto.response.ApiKeyResponse;
import com.ayshriv.recruitment.apiKey.entity.ApiKey;
import com.ayshriv.recruitment.apiKey.repository.ApiKeyRepository;
import com.ayshriv.recruitment.apiKey.security.ApiKeyAuthenticationException;
import com.ayshriv.recruitment.apiKey.security.ApiKeyPrincipal;
import com.ayshriv.recruitment.common.exception.ResourceNotFoundException;
import com.ayshriv.recruitment.organization.entity.Organization;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

/**
 * Business logic for API key lifecycle and authentication.
 */
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    /**
     * Prefix used for every generated key.
     */
    public static final String KEY_PREFIX_SEED = "sk_live_";

    /**
     * Number of random bytes backing a generated key.
     */
    private static final int RANDOM_BYTES = 16;

    /**
     * Characters of the raw key retained as the searchable prefix.
     */
    private static final int PREFIX_LENGTH = 12;

    /**
     * Reuse interval for the {@code lastUsedAt} heartbeat.
     */
    private static final long USAGE_TRACKING_INTERVAL_MINUTES = 5;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ApiKeyRepository apiKeyRepository;

    /**
     * Generate a cryptographically secure raw API key in the form
     * {@code sk_live_<32 hex>}.
     *
     * @return raw key, shown to the client only at creation time
     */
    public String generateKey() {
        byte[] bytes = new byte[RANDOM_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return KEY_PREFIX_SEED + HexFormat.of().formatHex(bytes);
    }

    /**
     * Compute the searchable prefix of a raw key.
     *
     * @param rawKey raw API key
     * @return first {@link #PREFIX_LENGTH} characters
     */
    public String extractKeyPrefix(String rawKey) {
        if (rawKey == null) {
            return "";
        }
        return rawKey.length() <= PREFIX_LENGTH ? rawKey : rawKey.substring(0, PREFIX_LENGTH);
    }

    /**
     * Compute the SHA-256 hex digest of a raw key.
     *
     * @param rawKey raw API key
     * @return 64 character lowercase hex digest
     */
    public String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawKey.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    /**
     * Validate a raw key and produce a Spring Security authentication.
     *
     * @param rawKey raw API key sent by the client
     * @return authentication carrying an {@link ApiKeyPrincipal}
     * @throws ApiKeyAuthenticationException when the key is invalid, inactive or expired
     */
    public Authentication authenticate(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) {
            throw new ApiKeyAuthenticationException("API key is required", "API_KEY_REQUIRED");
        }

        List<ApiKey> candidates = apiKeyRepository.findCandidatesByKeyPrefix(extractKeyPrefix(rawKey));

        String expectedHash = hashKey(rawKey);
        ApiKey matched = candidates.stream()
                .filter(apiKey -> MessageDigest.isEqual(
                        expectedHash.getBytes(StandardCharsets.UTF_8),
                        apiKey.getKeyHash().getBytes(StandardCharsets.UTF_8)))
                .findFirst()
                .orElseThrow(() -> new ApiKeyAuthenticationException("Invalid API key", "INVALID_API_KEY"));

        if (!matched.isActive()) {
            throw new ApiKeyAuthenticationException("API key is inactive", "API_KEY_INACTIVE");
        }
        if (matched.isExpired()) {
            throw new ApiKeyAuthenticationException("API key has expired", "API_KEY_EXPIRED");
        }

        ApiKeyPrincipal principal = new ApiKeyPrincipal(
                matched.getId(), matched.getOrganizationId(), matched.getName());

        return new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_API_KEY")));
    }

    /**
     * Record the last use time of a key, throttled to avoid a write per request.
     *
     * <p>Never throws: usage tracking must not break an otherwise valid request.</p>
     *
     * @param apiKeyId key id
     */
    public void markKeyUsed(Long apiKeyId) {
        if (apiKeyId == null) {
            return;
        }
        try {
            apiKeyRepository.findById(apiKeyId).ifPresent(apiKey -> {
                LocalDateTime now = LocalDateTime.now();
                if (apiKey.getLastUsedAt() == null
                        || now.minusMinutes(USAGE_TRACKING_INTERVAL_MINUTES).isAfter(apiKey.getLastUsedAt())) {
                    apiKey.setLastUsedAt(now);
                    apiKeyRepository.save(apiKey);
                }
            });
        } catch (RuntimeException ignored) {
            // never fail the request because usage tracking failed
        }
    }

    /**
     * Create a new API key and return the raw value once.
     *
     * @param organizationId owning tenant
     * @param request        creation payload
     * @return created key together with the raw key
     */
    @Transactional
    public ApiKeyCreatedResponse create(Long organizationId, CreateApiKeyRequest request) {
        String rawKey = generateKey();

        ApiKey apiKey = new ApiKey();
        apiKey.setName(request.getName());
        apiKey.setDescription(request.getDescription());
        apiKey.setExpiresAt(request.getExpiresAt());
        apiKey.setKeyPrefix(extractKeyPrefix(rawKey));
        apiKey.setKeyHash(hashKey(rawKey));
        apiKey.setOrganization(new Organization(organizationId));

        ApiKey saved = apiKeyRepository.save(apiKey);
        return toCreatedResponse(saved, rawKey);
    }

    /**
     * List all keys of an organization.
     *
     * @param organizationId owning tenant
     * @return keys without raw values
     */
    @Transactional(readOnly = true)
    public List<ApiKeyResponse> list(Long organizationId) {
        return apiKeyRepository.findAllByOrganizationId(organizationId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Get a single key of an organization.
     *
     * @param organizationId owning tenant
     * @param id             key primary key
     * @return key without raw value
     */
    @Transactional(readOnly = true)
    public ApiKeyResponse get(Long organizationId, Long id) {
        return toResponse(getEntity(organizationId, id));
    }

    /**
     * Update display attributes of a key.
     *
     * @param organizationId owning tenant
     * @param id             key primary key
     * @param request        update payload
     * @return updated key
     */
    @Transactional
    public ApiKeyResponse update(Long organizationId, Long id, UpdateApiKeyRequest request) {
        ApiKey apiKey = getEntity(organizationId, id);
        apiKey.setName(request.getName());
        apiKey.setDescription(request.getDescription());
        apiKey.setExpiresAt(request.getExpiresAt());
        return toResponse(apiKeyRepository.save(apiKey));
    }

    /**
     * Activate a key.
     *
     * @param organizationId owning tenant
     * @param id             key primary key
     * @return updated key
     */
    @Transactional
    public ApiKeyResponse activate(Long organizationId, Long id) {
        ApiKey apiKey = getEntity(organizationId, id);
        apiKey.activate();
        return toResponse(apiKeyRepository.save(apiKey));
    }

    /**
     * Deactivate a key.
     *
     * @param organizationId owning tenant
     * @param id             key primary key
     * @return updated key
     */
    @Transactional
    public ApiKeyResponse deactivate(Long organizationId, Long id) {
        ApiKey apiKey = getEntity(organizationId, id);
        apiKey.deactivate();
        return toResponse(apiKeyRepository.save(apiKey));
    }

    /**
     * Soft delete a key.
     *
     * @param organizationId owning tenant
     * @param id             key primary key
     */
    @Transactional
    public void delete(Long organizationId, Long id) {
        ApiKey apiKey = getEntity(organizationId, id);
        apiKey.softDelete();
        apiKeyRepository.save(apiKey);
    }

    private ApiKey getEntity(Long organizationId, Long id) {
        return apiKeyRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("API key not found", "API_KEY_NOT_FOUND"));
    }

    private ApiKeyResponse toResponse(ApiKey apiKey) {
        return ApiKeyResponse.builder()
                .id(apiKey.getId())
                .name(apiKey.getName())
                .keyPrefix(apiKey.getKeyPrefix() + "****")
                .description(apiKey.getDescription())
                .expiresAt(apiKey.getExpiresAt())
                .lastUsedAt(apiKey.getLastUsedAt())
                .organizationId(apiKey.getOrganizationId())
                .createdOn(apiKey.getCreatedOn())
                .updatedOn(apiKey.getUpdatedOn())
                .isActive(apiKey.isActive())
                .build();
    }

    private ApiKeyCreatedResponse toCreatedResponse(ApiKey apiKey, String rawKey) {
        return ApiKeyCreatedResponse.builder()
                .id(apiKey.getId())
                .name(apiKey.getName())
                .key(rawKey)
                .keyPrefix(apiKey.getKeyPrefix())
                .description(apiKey.getDescription())
                .expiresAt(apiKey.getExpiresAt())
                .organizationId(apiKey.getOrganizationId())
                .createdOn(apiKey.getCreatedOn())
                .build();
    }
}