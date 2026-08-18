package com.ayshriv.recruitment.organization.repository;

import com.ayshriv.recruitment.organization.entity.Organization;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Persistence access for {@link Organization}.
 *
 * <p>All queries use HQL / JPQL and parameter binding; user input is never
 * concatenated into queries.</p>
 */
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

    /**
     * Find a non deleted organization by primary key.
     *
     * @param id organization primary key
     * @return matching organization, if any
     */
    @Query("""
            SELECT o
            FROM Organization o
            WHERE o.id = :id
              AND o.isDeleted = false
            """)
    Optional<Organization> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * Find the active, non deleted organization by primary key.
     *
     * @param id organization primary key
     * @return matching active organization, if any
     */
    @Query("""
            SELECT o
            FROM Organization o
            WHERE o.id = :id
              AND o.isActive = true
              AND o.isDeleted = false
            """)
    Optional<Organization> findActiveOrganization(@Param("id") Long id);

    /**
     * Lock the organization row with a pessimistic write lock.
     *
     * <p>Used by the client module to serialize client code allocation per
     * tenant: concurrent client creations for the same organization queue on
     * this lock so that the next client code is always derived from the
     * committed state.</p>
     *
     * @param id organization primary key
     * @return matching organization, if any
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o
            FROM Organization o
            WHERE o.id = :id
            """)
    Optional<Organization> findByIdForUpdate(@Param("id") Long id);

    /**
     * Find a non deleted organization by email, ignoring case.
     *
     * @param email organization email
     * @return matching organization, if any
     */
    @Query("""
            SELECT o
            FROM Organization o
            WHERE LOWER(o.email) = LOWER(:email)
              AND o.isDeleted = false
            """)
    Optional<Organization> findByEmail(@Param("email") String email);

    /**
     * Page through all non deleted organizations, newest first.
     *
     * @param pageable pagination and sorting
     * @return page of non deleted organizations
     */
    @Query("""
            SELECT o
            FROM Organization o
            WHERE o.isDeleted = false
            ORDER BY o.createdOn DESC
            """)
    Page<Organization> findAllActiveOrganizations(Pageable pageable);

    /**
     * Search non deleted organizations by keyword across name, legal name
     * and email, ignoring case.
     *
     * @param keyword  search keyword
     * @param pageable pagination and sorting
     * @return page of matching organizations
     */
    @Query("""
            SELECT o
            FROM Organization o
            WHERE o.isDeleted = false
              AND (
                  LOWER(o.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(o.legalName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(o.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<Organization> searchOrganizations(@Param("keyword") String keyword, Pageable pageable);
}
