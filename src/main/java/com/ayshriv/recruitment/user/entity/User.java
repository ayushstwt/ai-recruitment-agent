package com.ayshriv.recruitment.user.entity;

import com.ayshriv.recruitment.common.entity.BaseEntity;
import com.ayshriv.recruitment.organization.entity.Organization;
import com.ayshriv.recruitment.role.entity.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Represents a person using the recruitment platform on behalf of an
 * organization.
 *
 * <p>A user always belongs to exactly one {@link Organization} and can never
 * be moved to another one; the tenant is the hard isolation boundary. Email
 * addresses are unique within an organization (enforced at the application
 * layer) but the same email may exist in different organizations.</p>
 *
 * <p>Passwords are never stored in plaintext: only the BCrypt
 * {@code passwordHash} is persisted and it is never serialized.</p>
 *
 * <p>The common persistence fields are inherited from {@link BaseEntity}.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "USERS")
public class User extends BaseEntity {

    /**
     * First name of the user.
     */
    @Basic(optional = false)
    @Column(name = "FIRST_NAME", nullable = false, length = 100)
    private String firstName;

    /**
     * Last name of the user.
     */
    @Basic(optional = false)
    @Column(name = "LAST_NAME", nullable = false, length = 100)
    private String lastName;

    /**
     * Login email, unique within the owning organization.
     */
    @Basic(optional = false)
    @Column(name = "EMAIL", nullable = false, length = 255)
    private String email;

    /**
     * Contact phone number.
     */
    @Column(name = "PHONE", length = 30)
    private String phone;

    /**
     * BCrypt hash of the user's password. Never reversible, never returned
     * through any API response.
     */
    @JsonIgnore
    @Basic(optional = false)
    @Column(name = "PASSWORD_HASH", nullable = false, length = 255)
    private String passwordHash;

    /**
     * Profile picture URL.
     */
    @Column(name = "PROFILE_IMAGE_URL", length = 500)
    private String profileImageUrl;

    /**
     * Display job title, for example {@code Senior Recruiter}.
     */
    @Column(name = "JOB_TITLE", length = 100)
    private String jobTitle;

    /**
     * Timestamp of the most recent successful login.
     */
    @Column(name = "LAST_LOGIN_AT")
    private LocalDateTime lastLoginAt;

    /**
     * Owning tenant. Never changed after creation.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ORGANIZATION_ID", nullable = false)
    private Organization organization;

    /**
     * Roles assigned to the user.
     *
     * <p>Lazy loaded and never serialized; the {@code USER_ROLES} join table
     * prevents a user from holding the same role twice.</p>
     */
    @JsonIgnore
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "USER_ROLES",
            joinColumns = @JoinColumn(name = "USER_ID"),
            inverseJoinColumns = @JoinColumn(name = "ROLE_ID"))
    private Set<Role> roles = new LinkedHashSet<>();

    /**
     * Owning tenant identifier, safe to access without initializing the lazy
     * relationship target.
     *
     * @return organization id or {@code null} when not set
     */
    public Long getOrganizationId() {
        return organization != null ? organization.getId() : null;
    }

    /**
     * Reference-only constructor used when setting associations by id.
     *
     * @param id user primary key
     */
    public User(Long id) {
        super(id);
    }
}