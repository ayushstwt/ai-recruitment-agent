package com.ayshriv.recruitment.organization.entity;

import com.ayshriv.recruitment.apiKey.entity.ApiKey;
import com.ayshriv.recruitment.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a tenant owning all resources in the application.
 *
 * <p>An {@code Organization} is an HR / Recruitment agency using the
 * platform. Every organization-owned resource (users, clients, jobs,
 * candidates, applications, interviews, documents, AI agents) must belong
 * to exactly one organization, making this entity the tenant boundary of
 * the whole application.</p>
 *
 * <p>The common persistence fields ({@code id}, {@code createdOn},
 * {@code updatedOn}, {@code isActive}, {@code isDeleted}) are inherited
 * from {@link BaseEntity}, which remains responsible for the entity
 * lifecycle.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ORGANIZATIONS")
public class Organization extends BaseEntity {

    /**
     * Human readable organization name.
     */
    @Basic(optional = false)
    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    /**
     * Registered / legal name of the organization.
     */
    @Column(name = "LEGAL_NAME", length = 200)
    private String legalName;

    /**
     * Primary contact email. Uniqueness is enforced at the application layer
     * so a soft-deleted organization does not block reuse of its email.
     */
    @Basic(optional = false)
    @Column(name = "EMAIL", nullable = false, length = 255)
    private String email;

    /**
     * Primary contact phone number.
     */
    @Column(name = "PHONE", length = 30)
    private String phone;

    /**
     * Organization website URL.
     */
    @Column(name = "WEBSITE", length = 255)
    private String website;

    /**
     * Short description of the organization.
     */
    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    /**
     * Industry sector, for example {@code Recruitment}.
     */
    @Column(name = "INDUSTRY", length = 100)
    private String industry;

    /**
     * Country where the organization operates.
     */
    @Column(name = "COUNTRY", length = 100)
    private String country;

    /**
     * State / province where the organization operates.
     */
    @Column(name = "STATE", length = 100)
    private String state;

    /**
     * City where the organization operates.
     */
    @Column(name = "CITY", length = 100)
    private String city;

    /**
     * IANA timezone of the organization, for example {@code Asia/Kolkata}.
     */
    @Column(name = "TIMEZONE", length = 50)
    private String timezone;

    /**
     * API keys issued to this organization.
     *
     * <p>Lazy loaded and never serialized so it cannot create circular JSON
     * relationships. {@link ApiKey} owns the foreign key.</p>
     */
    @JsonIgnore
    @OneToMany(mappedBy = "organization", fetch = FetchType.LAZY)
    private List<ApiKey> apiKeys = new ArrayList<>();

    /**
     * Reference-only constructor used when setting associations by id.
     *
     * @param id organization primary key
     */
    public Organization(Long id) {
        super(id);
    }

    /**
     * Convenience constructor for creation flows.
     *
     * @param name organization name
     */
    public Organization(String name) {
        this.name = name;
    }
}
