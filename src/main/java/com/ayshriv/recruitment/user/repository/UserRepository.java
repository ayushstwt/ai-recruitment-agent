package com.ayshriv.recruitment.user.repository;

import com.ayshriv.recruitment.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Persistence access for {@link User}.
 *
 * <p>All queries use HQL / JPQL and parameter binding. Every lookup is
 * tenant-aware: users are always scoped to an organization id and soft-deleted
 * users are never returned. The email lookup is organization-scoped because
 * the same email may legitimately exist in different organizations.</p>
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a non deleted user by primary key, regardless of its tenant. Used
     * by the service to distinguish a cross-organization user (forbidden)
     * from a missing one (not found).
     *
     * @param id user primary key
     * @return matching user, if any
     */
    @Query("""
            SELECT u
            FROM User u
            WHERE u.id = :id
              AND u.isDeleted = false
            """)
    Optional<User> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * Find a non deleted user by primary key within a single organization.
     *
     * @param userId         user primary key
     * @param organizationId owning tenant
     * @return matching user, if any
     */
    @Query("""
            SELECT u
            FROM User u
            WHERE u.id = :userId
              AND u.organization.id = :organizationId
              AND u.isDeleted = false
            """)
    Optional<User> findByIdAndOrganization(@Param("userId") Long userId,
                                           @Param("organizationId") Long organizationId);

    /**
     * Find an active, non deleted user by primary key within an organization.
     *
     * @param userId         user primary key
     * @param organizationId owning tenant
     * @return matching active user, if any
     */
    @Query("""
            SELECT u
            FROM User u
            WHERE u.id = :userId
              AND u.organization.id = :organizationId
              AND u.isActive = true
              AND u.isDeleted = false
            """)
    Optional<User> findActiveByIdAndOrganization(@Param("userId") Long userId,
                                                 @Param("organizationId") Long organizationId);

    /**
     * Find a non deleted user by email within a single organization, ignoring
     * case. Never searches by email globally because the same email may exist
     * in different organizations.
     *
     * @param email          user email
     * @param organizationId owning tenant
     * @return matching user, if any
     */
    @Query("""
            SELECT u
            FROM User u
            WHERE LOWER(u.email) = LOWER(:email)
              AND u.organization.id = :organizationId
              AND u.isDeleted = false
            """)
    Optional<User> findByEmailAndOrganization(@Param("email") String email,
                                              @Param("organizationId") Long organizationId);

    /**
     * Page through all non deleted users of an organization, newest first.
     *
     * @param organizationId owning tenant
     * @param pageable       pagination and sorting
     * @return page of non deleted users
     */
    @Query("""
            SELECT u
            FROM User u
            WHERE u.organization.id = :organizationId
              AND u.isDeleted = false
            ORDER BY u.createdOn DESC
            """)
    Page<User> findAllByOrganization(@Param("organizationId") Long organizationId, Pageable pageable);

    /**
     * Search non deleted users of an organization by keyword across first
     * name, last name, email and job title, ignoring case.
     *
     * @param organizationId owning tenant
     * @param keyword        search keyword
     * @param pageable       pagination and sorting
     * @return page of matching users
     */
    @Query("""
            SELECT u
            FROM User u
            WHERE u.organization.id = :organizationId
              AND u.isDeleted = false
              AND (
                  LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(u.jobTitle) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<User> searchUsers(@Param("organizationId") Long organizationId,
                           @Param("keyword") String keyword, Pageable pageable);

    /**
     * Page through non deleted users of an organization holding a given role.
     *
     * @param roleId         role primary key
     * @param organizationId owning tenant
     * @param pageable       pagination and sorting
     * @return page of users holding the role
     */
    @Query("""
            SELECT u
            FROM User u
            JOIN u.roles r
            WHERE u.organization.id = :organizationId
              AND u.isDeleted = false
              AND r.id = :roleId
            """)
    Page<User> findByRoleIdAndOrganization(@Param("roleId") Long roleId,
                                           @Param("organizationId") Long organizationId,
                                           Pageable pageable);
}