package com.ayshriv.recruitment.common.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base entity containing common persistence and lifecycle fields
 * shared by all JPA entities.
 *
 * <p>Provides:</p>
 * <ul>
 *     <li>Auto-generated primary key</li>
 *     <li>Created timestamp</li>
 *     <li>Updated timestamp</li>
 *     <li>Active flag</li>
 *     <li>Soft-delete flag</li>
 * </ul>
 *
 * <p>All persistent business entities must extend this class.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@MappedSuperclass
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ID", nullable = false)
    @JsonProperty("id")
    private Long id;

    /**
     * Timestamp when the entity was created.
     */
    @Basic(optional = false)
    @Column(
            name = "CREATED_ON",
            nullable = false,
            updatable = false
    )
    @JsonProperty("created_on")
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "dd-MM-yyyy HH:mm:ss",
            timezone = "Asia/Kolkata"
    )
    private LocalDateTime createdOn;

    /**
     * Timestamp when the entity was last updated.
     */
    @Basic(optional = false)
    @Column(
            name = "UPDATED_ON",
            nullable = false
    )
    @JsonProperty("updated_on")
    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "dd-MM-yyyy HH:mm:ss",
            timezone = "Asia/Kolkata"
    )
    private LocalDateTime updatedOn;

    /**
     * Indicates whether the entity is active.
     */
    @JsonIgnore
    @Basic(optional = false)
    @Column(
            name = "IS_ACTIVE",
            nullable = false
    )
    private boolean isActive;

    /**
     * Indicates whether the entity has been soft deleted.
     */
    @JsonIgnore
    @Basic(optional = false)
    @Column(
            name = "IS_DELETED",
            nullable = false
    )
    private boolean isDeleted;

    /**
     * Convenience constructor for entity references.
     */
    public BaseEntity(Long id) {
        this.id = id;
    }

    /**
     * JPA lifecycle callback executed before insert.
     */
    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        this.createdOn = now;
        this.updatedOn = now;

        this.isActive = true;
        this.isDeleted = false;
    }

    /**
     * JPA lifecycle callback executed before update.
     */
    @PreUpdate
    protected void onUpdate() {

        this.updatedOn = LocalDateTime.now();
    }

    /**
     * Indicates whether this entity is new.
     *
     * @return true when the entity does not have an ID
     */
    @JsonIgnore
    public boolean isNew() {

        return this.id == null;
    }

    /**
     * Activate entity.
     */
    public void activate() {

        this.isActive = true;
        this.isDeleted = false;
    }

    /**
     * Deactivate entity without deleting it.
     */
    public void deactivate() {

        this.isActive = false;
    }

    /**
     * Perform a soft delete.
     */
    public void softDelete() {

        this.isActive = false;
        this.isDeleted = true;
    }
}