package com.ayshriv.recruitment.role.entity;

import com.ayshriv.recruitment.common.entity.BaseEntity;
import com.ayshriv.recruitment.organization.entity.Organization;
import com.ayshriv.recruitment.user.entity.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Represents a permission set that can be assigned to users.
 *
 * <p>Roles follow a dual model:</p>
 * <ul>
 *     <li><b>System roles</b> ({@code isSystemRole = true},
 *     {@code organization = null}) are global application-level roles shared
 *     by every organization, for example {@code AGENCY_ADMIN}.</li>
 *     <li><b>Organization roles</b> ({@code isSystemRole = false}) are tenant
 *     scoped; a user of organization A can never be assigned a role that
 *     belongs to organization B.</li>
 * </ul>
 *
 * <p>The common persistence fields are inherited from {@link BaseEntity}.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ROLES")
public class Role extends BaseEntity {

    /**
     * Machine readable role name, for example {@code RECRUITER}.
     */
    @Basic(optional = false)
    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    /**
     * Human readable description of the role's responsibilities.
     */
    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    /**
     * Whether this role is a global system role. System roles cannot be
     * modified by normal organization users.
     */
    @Basic(optional = false)
    @Column(name = "IS_SYSTEM_ROLE", nullable = false)
    private boolean isSystemRole;

    /**
     * Owning tenant. {@code null} for system roles, which are shared globally.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORGANIZATION_ID")
    private Organization organization;

    /**
     * Users holding this role.
     *
     * <p>Lazy loaded and never serialized; {@code User} owns the join table.</p>
     */
    @JsonIgnore
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<User> users = new LinkedHashSet<>();

    /**
     * Owning tenant identifier, safe to access without initializing the
     * lazy relationship target.
     *
     * @return organization id, or {@code null} for system roles
     */
    public Long getOrganizationId() {
        return organization != null ? organization.getId() : null;
    }

    /**
     * Reference-only constructor used when setting associations by id.
     *
     * @param id role primary key
     */
    public Role(Long id) {
        super(id);
    }
}