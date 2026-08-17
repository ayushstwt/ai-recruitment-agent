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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    private static final String RAW_KEY = apiKeyFromEnv("TEST_API_KEY", "a1b2c3d4e5f60718293a4b5c6d7e8f90");
    private static final String OTHER_KEY = apiKeyFromEnv("TEST_API_KEY_OTHER", "ffffffffffffffffffffffffffffffff");

    private static String apiKeyFromEnv(String name, String fallbackHex) {
        String fromEnv = System.getenv(name);
        return (fromEnv != null && !fromEnv.isBlank()) ? fromEnv : ApiKeyService.KEY_PREFIX_SEED + fallbackHex;
    }

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @InjectMocks
    private ApiKeyService apiKeyService;

    private ApiKey key(Long id, String keyHash, boolean active, boolean deleted,
                       LocalDateTime expiresAt, Long organizationId) {
        ApiKey key = new ApiKey();
        key.setId(id);
        key.setName("prod-key");
        key.setKeyPrefix(apiKeyService.extractKeyPrefix(RAW_KEY));
        key.setKeyHash(keyHash);
        key.setActive(active);
        key.setDeleted(deleted);
        key.setExpiresAt(expiresAt);
        key.setOrganization(new Organization(organizationId));
        return key;
    }

    @Test
    void generateKeyProducesSecureFormat() {
        String key = apiKeyService.generateKey();
        assertThat(key).startsWith(ApiKeyService.KEY_PREFIX_SEED);
        assertThat(key).hasSize(ApiKeyService.KEY_PREFIX_SEED.length() + 32);
        assertThat(key).matches(Pattern.compile("^sk_live_[0-9a-f]{32}$"));
    }

    @Test
    void generatedKeysAreUnique() {
        assertThat(apiKeyService.generateKey()).isNotEqualTo(apiKeyService.generateKey());
    }

    @Test
    void hashKeyIsDeterministicSha256() {
        String first = apiKeyService.hashKey(RAW_KEY);
        String second = apiKeyService.hashKey(RAW_KEY);
        assertThat(first).isEqualTo(second).hasSize(64);
        assertThat(first).matches(Pattern.compile("^[0-9a-f]{64}$"));
        assertThat(first).isNotEqualTo(RAW_KEY);
    }

    @Test
    void extractKeyPrefixReturnsFirstTwelveCharacters() {
        assertThat(apiKeyService.extractKeyPrefix(RAW_KEY)).isEqualTo("sk_live_a1b2");
    }

    @Test
    void authenticateReturnsPrincipalForValidKey() {
        String hash = apiKeyService.hashKey(RAW_KEY);
        ApiKey key = key(42L, hash, true, false, null, 10L);
        when(apiKeyRepository.findCandidatesByKeyPrefix(anyString())).thenReturn(List.of(key));

        Authentication authentication = apiKeyService.authenticate(RAW_KEY);

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isInstanceOf(ApiKeyPrincipal.class);
        ApiKeyPrincipal principal = (ApiKeyPrincipal) authentication.getPrincipal();
        assertThat(principal.apiKeyId()).isEqualTo(42L);
        assertThat(principal.organizationId()).isEqualTo(10L);
        assertThat(principal.keyName()).isEqualTo("prod-key");
    }

    @Test
    void authenticateRejectsMissingKey() {
        assertThatThrownBy(() -> apiKeyService.authenticate(null))
                .isInstanceOf(ApiKeyAuthenticationException.class)
                .hasMessage("API key is required")
                .extracting(ex -> ((ApiKeyAuthenticationException) ex).getCode())
                .isEqualTo("API_KEY_REQUIRED");
        assertThatThrownBy(() -> apiKeyService.authenticate("   "))
                .isInstanceOf(ApiKeyAuthenticationException.class)
                .extracting(ex -> ((ApiKeyAuthenticationException) ex).getCode())
                .isEqualTo("API_KEY_REQUIRED");
    }

    @Test
    void authenticateRejectsUnknownKey() {
        when(apiKeyRepository.findCandidatesByKeyPrefix(anyString())).thenReturn(List.of());

        assertThatThrownBy(() -> apiKeyService.authenticate(RAW_KEY))
                .isInstanceOf(ApiKeyAuthenticationException.class)
                .extracting(ex -> ((ApiKeyAuthenticationException) ex).getCode())
                .isEqualTo("INVALID_API_KEY");
    }

    @Test
    void authenticateRejectsKeyWithDifferentHash() {
        ApiKey key = key(1L, apiKeyService.hashKey(OTHER_KEY), true, false, null, 10L);
        when(apiKeyRepository.findCandidatesByKeyPrefix(anyString())).thenReturn(List.of(key));

        assertThatThrownBy(() -> apiKeyService.authenticate(RAW_KEY))
                .isInstanceOf(ApiKeyAuthenticationException.class)
                .extracting(ex -> ((ApiKeyAuthenticationException) ex).getCode())
                .isEqualTo("INVALID_API_KEY");
    }

    @Test
    void authenticateRejectsInactiveKey() {
        ApiKey key = key(1L, apiKeyService.hashKey(RAW_KEY), false, false, null, 10L);
        when(apiKeyRepository.findCandidatesByKeyPrefix(anyString())).thenReturn(List.of(key));

        assertThatThrownBy(() -> apiKeyService.authenticate(RAW_KEY))
                .isInstanceOf(ApiKeyAuthenticationException.class)
                .extracting(ex -> ((ApiKeyAuthenticationException) ex).getCode())
                .isEqualTo("API_KEY_INACTIVE");
    }

    @Test
    void authenticateRejectsExpiredKey() {
        ApiKey key = key(1L, apiKeyService.hashKey(RAW_KEY), true, false,
                LocalDateTime.now().minusMinutes(1), 10L);
        when(apiKeyRepository.findCandidatesByKeyPrefix(anyString())).thenReturn(List.of(key));

        assertThatThrownBy(() -> apiKeyService.authenticate(RAW_KEY))
                .isInstanceOf(ApiKeyAuthenticationException.class)
                .extracting(ex -> ((ApiKeyAuthenticationException) ex).getCode())
                .isEqualTo("API_KEY_EXPIRED");
    }

    @Test
    void createStoresOnlyHashAndReturnsRawKeyOnce() {
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiKeyCreatedResponse response = apiKeyService.create(
                10L, new CreateApiKeyRequest("prod-key", "production", null));

        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepository).save(captor.capture());
        ApiKey saved = captor.getValue();

        assertThat(response.getKey()).startsWith("sk_live_");
        assertThat(response.getOrganizationId()).isEqualTo(10L);
        assertThat(response.getKeyPrefix()).isEqualTo(response.getKey().substring(0, 12));
        assertThat(saved.getKeyHash()).isEqualTo(apiKeyService.hashKey(response.getKey()));
        assertThat(saved.getKeyHash()).isNotEqualTo(response.getKey());
        assertThat(saved.getOrganization().getId()).isEqualTo(10L);
    }

    @Test
    void listReturnsKeysForOrganizationOnly() {
        ApiKey key = key(1L, "hash", true, false, null, 10L);
        when(apiKeyRepository.findAllByOrganizationId(10L)).thenReturn(List.of(key));

        List<ApiKeyResponse> keys = apiKeyService.list(10L);

        assertThat(keys).hasSize(1);
        assertThat(keys.get(0).getId()).isEqualTo(1L);
        assertThat(keys.get(0).getKeyPrefix()).endsWith("****");
    }

    @Test
    void getReturnsKeyScopedToOrganization() {
        ApiKey key = key(7L, "hash", true, false, null, 10L);
        when(apiKeyRepository.findByIdAndOrganizationId(7L, 10L)).thenReturn(Optional.of(key));

        ApiKeyResponse response = apiKeyService.get(10L, 7L);

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getOrganizationId()).isEqualTo(10L);
        verify(apiKeyRepository).findByIdAndOrganizationId(7L, 10L);
    }

    @Test
    void getThrowsWhenKeyDoesNotBelongToOrganization() {
        when(apiKeyRepository.findByIdAndOrganizationId(7L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> apiKeyService.get(10L, 7L))
                .isInstanceOf(ResourceNotFoundException.class)
                .extracting(ex -> ((ResourceNotFoundException) ex).getCode())
                .isEqualTo("API_KEY_NOT_FOUND");
    }

    @Test
    void updateChangesOnlyDisplayAttributes() {
        ApiKey key = key(1L, "original-hash", true, false, null, 10L);
        when(apiKeyRepository.findByIdAndOrganizationId(1L, 10L)).thenReturn(Optional.of(key));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiKeyResponse response = apiKeyService.update(10L, 1L,
                new UpdateApiKeyRequest("renamed", "new description", LocalDateTime.now().plusDays(1)));

        assertThat(response.getName()).isEqualTo("renamed");
        assertThat(response.getDescription()).isEqualTo("new description");
        assertThat(response.getExpiresAt()).isNotNull();
        assertThat(key.getKeyHash()).isEqualTo("original-hash");
    }

    @Test
    void deactivateDisablesKey() {
        ApiKey key = key(1L, "hash", true, false, null, 10L);
        when(apiKeyRepository.findByIdAndOrganizationId(1L, 10L)).thenReturn(Optional.of(key));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiKeyResponse response = apiKeyService.deactivate(10L, 1L);

        assertThat(response.isActive()).isFalse();
        assertThat(key.isActive()).isFalse();
    }

    @Test
    void activateEnablesKey() {
        ApiKey key = key(1L, "hash", false, false, null, 10L);
        when(apiKeyRepository.findByIdAndOrganizationId(1L, 10L)).thenReturn(Optional.of(key));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiKeyResponse response = apiKeyService.activate(10L, 1L);

        assertThat(response.isActive()).isTrue();
        assertThat(key.isDeleted()).isFalse();
    }

    @Test
    void deleteSoftDeletesKey() {
        ApiKey key = key(1L, "hash", true, false, null, 10L);
        when(apiKeyRepository.findByIdAndOrganizationId(1L, 10L)).thenReturn(Optional.of(key));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> invocation.getArgument(0));

        apiKeyService.delete(10L, 1L);

        assertThat(key.isDeleted()).isTrue();
        assertThat(key.isActive()).isFalse();
    }

    @Test
    void markKeyUsedUpdatesOnlyWhenStale() {
        ApiKey fresh = key(1L, "hash", true, false, null, 10L);
        fresh.setLastUsedAt(LocalDateTime.now());
        when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(fresh));

        apiKeyService.markKeyUsed(1L);

        verify(apiKeyRepository, never()).save(any(ApiKey.class));
    }

    @Test
    void markKeyUsedRecordsFirstUse() {
        ApiKey fresh = key(1L, "hash", true, false, null, 10L);
        when(apiKeyRepository.findById(1L)).thenReturn(Optional.of(fresh));

        apiKeyService.markKeyUsed(1L);

        verify(apiKeyRepository).save(fresh);
        assertThat(fresh.getLastUsedAt()).isNotNull();
    }
}