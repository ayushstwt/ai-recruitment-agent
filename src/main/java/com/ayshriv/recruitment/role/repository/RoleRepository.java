package com.ayshriv.recruitment.role.repository;

import com.ayshriv.recruitment.role.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Persistence access for {@link Role}.
 *
 * <p>All queries use HQL / JPQL and parameter binding. Every lookup is
 * tenant-aware: organization roles are always scoped to an organization id
 * while system roles may be shared globally.</p>
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Find a non deleted role by primary key, regardless of its tenant.
     *
     * @param id role primary key
     * @return matching role, if any
     */
    @Query("""
            SELECT r
            FROM Role r
            WHERE r.id = :id
              AND r.isDeleted = false
            """)
    Optional<Role> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * Find an active, non deleted role by primary key.
     *
     * @param id role primary key
     * @return matching active role, if any
     */
    @Query("""
            SELECT r
            FROM Role r
            WHERE r.id = :id
              AND r.isActive = true
              AND r.isDeleted = false
            """)
    Optional<Role> findActiveRole(@Param("id") Long id);

    /**
     * Find a non deleted role the given organization is allowed to use: either
     * an organization role of the tenant itself or any system role.
     *
     * @param roleId         role primary key
     * @param organizationId owning tenant
     * @return accessible role, if any
     */
    @Query("""
            SELECT r
            FROM Role r
            WHERE r.id = :roleId
              AND r.isDeleted = false
              AND (
                  r.organization.id = :organizationId
                  OR r.isSystemRole = true
              )
            """)
    Optional<Role> findAccessibleRole(@Param("roleId") Long roleId,
                                      @Param("organizationId") Long organizationId);

    /**
     * List every non deleted role the organization can use: its own
     * organization roles plus the globally shared system roles.
     *
     * @param organizationId owning tenant
     * @return accessible roles, ordered by name
     */
    @Query("""
            SELECT r
            FROM Role r
            WHERE r.isDeleted = false
              AND (
                  r.organization.id = :organizationId
                  OR r.isSystemRole = true
              )
            ORDER BY r.name ASC
            """)
    List<Role> findAccessibleRoles(@Param("organizationId") Long organizationId);

    /**
     * List every non deleted system role.
     *
     * @return system roles, ordered by name
     */
    @Query("""
            SELECT r
            FROM Role r
            WHERE r.isDeleted = false
              AND r.isSystemRole = true
            ORDER BY r.name ASC
            """)
    List<Role> findSystemRoles();

    /**
     * Find an organization role by name within a single tenant, ignoring case.
     *
     * @param name           role name
     * @param organizationId owning tenant
     * @return matching organization role, if any
     */
    @Query("""
            SELECT r
            FROM Role r
            WHERE LOWER(r.name) = LOWER(:name)
              AND r.isDeleted = false
              AND r.isSystemRole = false
              AND r.organization.id = :organizationId
            """)
    Optional<Role> findByNameAndOrganization(@Param("name") String name,
                                             @Param("organizationId") Long organizationId);
}