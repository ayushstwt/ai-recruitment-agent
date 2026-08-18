package com.ayshriv.recruitment.client.repository;

import com.ayshriv.recruitment.client.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Persistence access for {@link Client}.
 *
 * <p>All queries use HQL / JPQL and parameter binding; user input is never
 * concatenated into queries. Every tenant-owned lookup is scoped to an
 * organization id and soft-deleted clients are never returned, making the
 * owning tenant the hard isolation boundary.</p>
 */
public interface ClientRepository extends JpaRepository<Client, Long>, JpaSpecificationExecutor<Client> {

    /**
     * Find a non deleted client by primary key, regardless of its tenant. Used
     * by the service to distinguish a cross-organization client (forbidden)
     * from a missing one (not found).
     *
     * @param id client primary key
     * @return matching client, if any
     */
    @Query("""
            SELECT c
            FROM Client c
            WHERE c.id = :id
              AND c.isDeleted = false
            """)
    Optional<Client> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * Find a non deleted client by primary key within a single organization.
     *
     * @param clientId       client primary key
     * @param organizationId owning tenant
     * @return matching client, if any
     */
    @Query("""
            SELECT c
            FROM Client c
            WHERE c.id = :clientId
              AND c.organization.id = :organizationId
              AND c.isDeleted = false
            """)
    Optional<Client> findByIdAndOrganization(@Param("clientId") Long clientId,
                                             @Param("organizationId") Long organizationId);

    /**
     * Whether a non deleted client with the given id exists in the
     * organization. Backs the tenant ownership check for id based lookups.
     *
     * @param clientId       client primary key
     * @param organizationId owning tenant
     * @return {@code true} when the client exists in the organization
     */
    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN TRUE ELSE FALSE END
            FROM Client c
            WHERE c.id = :clientId
              AND c.organization.id = :organizationId
              AND c.isDeleted = false
            """)
    boolean existsByIdAndOrganization(@Param("clientId") Long clientId,
                                      @Param("organizationId") Long organizationId);

    /**
     * Find a non deleted client by client code within a single organization,
     * ignoring case.
     *
     * @param clientCode     human readable client code
     * @param organizationId owning tenant
     * @return matching client, if any
     */
    @Query("""
            SELECT c
            FROM Client c
            WHERE LOWER(c.clientCode) = LOWER(:clientCode)
              AND c.organization.id = :organizationId
              AND c.isDeleted = false
            """)
    Optional<Client> findByClientCodeAndOrganization(@Param("clientCode") String clientCode,
                                                     @Param("organizationId") Long organizationId);

    /**
     * Find a non deleted client by email within a single organization, ignoring
     * case. Never searches by email globally because the same email may exist
     * in different organizations.
     *
     * @param email          client email
     * @param organizationId owning tenant
     * @return matching client, if any
     */
    @Query("""
            SELECT c
            FROM Client c
            WHERE LOWER(c.email) = LOWER(:email)
              AND c.organization.id = :organizationId
              AND c.isDeleted = false
            """)
    Optional<Client> findByEmailAndOrganization(@Param("email") String email,
                                                @Param("organizationId") Long organizationId);

    /**
     * Count every client ever created in the organization, including soft
     * deleted ones. Client codes are allocated sequentially and never reused,
     * so this total equals the highest allocated code number and is the base
     * for generating the next code.
     *
     * @param organizationId owning tenant
     * @return total number of client rows in the organization
     */
    @Query("""
            SELECT COUNT(c)
            FROM Client c
            WHERE c.organization.id = :organizationId
            """)
    long countByOrganization(@Param("organizationId") Long organizationId);

    /**
     * Page through all non deleted clients of an organization, newest first.
     *
     * @param organizationId owning tenant
     * @param pageable       pagination and sorting
     * @return page of non deleted clients
     */
    @Query("""
            SELECT c
            FROM Client c
            WHERE c.organization.id = :organizationId
              AND c.isDeleted = false
            ORDER BY c.createdOn DESC
            """)
    Page<Client> findAllByOrganization(@Param("organizationId") Long organizationId, Pageable pageable);

    /**
     * Page through all active, non deleted clients of an organization.
     *
     * @param organizationId owning tenant
     * @param pageable       pagination and sorting
     * @return page of active clients
     */
    @Query("""
            SELECT c
            FROM Client c
            WHERE c.organization.id = :organizationId
              AND c.isActive = true
              AND c.isDeleted = false
            ORDER BY c.createdOn DESC
            """)
    Page<Client> findActiveClients(@Param("organizationId") Long organizationId, Pageable pageable);

    /**
     * Search non deleted clients of an organization by keyword across company
     * name, legal name, email, client code and industry, ignoring case.
     *
     * @param organizationId owning tenant
     * @param keyword        search keyword
     * @param pageable       pagination and sorting
     * @return page of matching clients
     */
    @Query("""
            SELECT c
            FROM Client c
            WHERE c.organization.id = :organizationId
              AND c.isDeleted = false
              AND (
                  LOWER(c.companyName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(c.legalName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(c.clientCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(c.industry) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<Client> searchClients(@Param("organizationId") Long organizationId,
                               @Param("keyword") String keyword, Pageable pageable);
}