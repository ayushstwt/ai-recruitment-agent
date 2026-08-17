package com.ayshriv.recruitment.apiKey.entity;

import com.ayshriv.recruitment.common.entity.BaseEntity;
import com.ayshriv.recruitment.organization.entity.Organization;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A hashed API key used for header based authentication.
 *
 * <p>The raw key is generated once and shown to the client at creation
 * time only. This entity persists the SHA-256 hash plus a short prefix
 * used to quickly narrow candidate keys during lookup. The raw value is
 * never stored.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "API_KEYS")
public class ApiKey extends BaseEntity {

    /**
     * Display name for the key.
     */
    @Basic(optional = false)
    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    /**
     * First {@code sk_live_} + 4 hex characters of the raw key, used for indexing.
     */
    @Basic(optional = false)
    @Column(name = "KEY_PREFIX", nullable = false, updatable = false, length = 12)
    private String keyPrefix;

    /**
     * SHA-256 hex digest of the raw key. Never reversible.
     */
    @Basic(optional = false)
    @Column(name = "KEY_HASH", nullable = false, updatable = false, length = 64)
    private String keyHash;

    /**
     * Optional expiry; a key past this timestamp is rejected.
     */
    @Column(name = "EXPIRES_AT")
    private LocalDateTime expiresAt;

    /**
     * Timestamp of the most recent successful authentication.
     */
    @Column(name = "LAST_USED_AT")
    private LocalDateTime lastUsedAt;

    /**
     * Optional human readable description.
     */
    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    /**
     * Owning tenant.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ORGANIZATION_ID", nullable = false)
    private Organization organization;

    /**
     * Owning tenant identifier, safe to serialize.
     *
     * @return organization id or {@code null} when not set
     */
    public Long getOrganizationId() {
        return organization != null ? organization.getId() : null;
    }

    /**
     * Whether the key has passed its expiry timestamp.
     *
     * @return {@code true} when expired
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}