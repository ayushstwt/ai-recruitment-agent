package com.ayshriv.recruitment.clientContact.repository;

import com.ayshriv.recruitment.clientContact.entity.ClientContact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Persistence access for {@link ClientContact}.
 *
 * <p>All queries use HQL / JPQL and parameter binding; user input is never
 * concatenated into queries. Tenant isolation is inherited through the owning
 * client: every query scopes contacts by {@code client.organization.id} and
 * soft-deleted contacts are never returned.</p>
 */
public interface ClientContactRepository extends JpaRepository<ClientContact, Long> {

    /**
     * Find a non deleted contact by primary key, regardless of its tenant.
     * Used by the service to distinguish a cross-organization contact
     * (forbidden) from a missing one (not found).
     *
     * @param id contact primary key
     * @return matching contact, if any
     */
    @Query("""
            SELECT c
            FROM ClientContact c
            WHERE c.id = :id
              AND c.isDeleted = false
            """)
    Optional<ClientContact> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * Whether a non deleted contact with the given id belongs to the
     * organization. Backs the tenant ownership check for id based lookups.
     *
     * @param contactId      contact primary key
     * @param organizationId owning tenant
     * @return {@code true} when the contact belongs to the organization
     */
    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END
            FROM ClientContact c
            WHERE c.id = :contactId
              AND c.client.organization.id = :organizationId
              AND c.isDeleted = false
            """)
    boolean existsByIdAndOrganization(@Param("contactId") Long contactId,
                                      @Param("organizationId") Long organizationId);

    /**
     * Page through all non deleted contacts of a client within an
     * organization, newest first.
     *
     * @param clientId       owning client
     * @param organizationId owning tenant
     * @param pageable       pagination and sorting
     * @return page of contacts
     */
    @Query("""
            SELECT c
            FROM ClientContact c
            WHERE c.client.id = :clientId
              AND c.client.organization.id = :organizationId
              AND c.isDeleted = false
            ORDER BY c.createdOn DESC
            """)
    Page<ClientContact> findAllByClientIdAndOrganization(@Param("clientId") Long clientId,
                                                         @Param("organizationId") Long organizationId,
                                                         Pageable pageable);

    /**
     * Search non deleted contacts of a client within an organization by
     * keyword across first name, last name, email, job title and department,
     * ignoring case.
     *
     * @param clientId       owning client
     * @param organizationId owning tenant
     * @param keyword        search keyword
     * @param pageable       pagination and sorting
     * @return page of matching contacts
     */
    @Query("""
            SELECT c
            FROM ClientContact c
            WHERE c.client.id = :clientId
              AND c.client.organization.id = :organizationId
              AND c.isDeleted = false
              AND (
                  LOWER(c.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(c.jobTitle) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(c.department) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<ClientContact> searchContacts(@Param("clientId") Long clientId,
                                       @Param("organizationId") Long organizationId,
                                       @Param("keyword") String keyword, Pageable pageable);
}