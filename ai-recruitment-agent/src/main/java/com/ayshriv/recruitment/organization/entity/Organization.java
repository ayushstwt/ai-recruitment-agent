package com.ayshriv.recruitment.organization.entity;

import com.ayshriv.recruitment.common.entity.BaseEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a tenant owning all resources in the application.
 *
 * <p>The organization feature itself is not fully implemented yet. This
 * minimal entity exists so that multi-tenant resources (such as
 * {@code ApiKey}) can hold a valid foreign key and the schema can be
 * validated by Hibernate. Expand it as part of the organization feature.</p>
 */
@Getter
@Setter
@NoArgsConstructor
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