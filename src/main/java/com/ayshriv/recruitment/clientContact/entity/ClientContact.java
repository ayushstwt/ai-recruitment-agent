package com.ayshriv.recruitment.clientContact.entity;

import com.ayshriv.recruitment.client.entity.Client;
import com.ayshriv.recruitment.common.entity.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a contact inside a client company.
 *
 * <p>A recruitment agency may hold multiple contacts per client, so every
 * contact belongs to exactly one {@link Client}. Tenant isolation is inherited
 * through the owning client: contacts are always scoped via
 * {@code client.organization.id}, never by a bare contact id.</p>
 *
 * <p>The common persistence fields are inherited from {@link BaseEntity}.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "CLIENT_CONTACTS")
public class ClientContact extends BaseEntity {

    /**
     * First name of the contact.
     */
    @Basic(optional = false)
    @Column(name = "FIRST_NAME", nullable = false, length = 100)
    private String firstName;

    /**
     * Last name of the contact.
     */
    @Basic(optional = false)
    @Column(name = "LAST_NAME", nullable = false, length = 100)
    private String lastName;

    /**
     * Contact email. The same email may exist for different clients and in
     * different organizations; no global uniqueness is enforced.
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
     * Job title, for example {@code HR Manager}.
     */
    @Column(name = "JOB_TITLE", length = 100)
    private String jobTitle;

    /**
     * Department, for example {@code Human Resources}.
     */
    @Column(name = "DEPARTMENT", length = 100)
    private String department;

    /**
     * Internal recruitment notes about the contact.
     */
    @Column(name = "NOTES", length = 2000)
    private String notes;

    /**
     * Client company the contact belongs to. Never changed after creation.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CLIENT_ID", nullable = false)
    private Client client;

    /**
     * Owning client identifier, safe to access without initializing the lazy
     * relationship target.
     *
     * @return client id or {@code null} when not set
     */
    public Long getClientId() {
        return client != null ? client.getId() : null;
    }

    /**
     * Reference-only constructor used when setting associations by id.
     *
     * @param id contact primary key
     */
    public ClientContact(Long id) {
        super(id);
    }
}