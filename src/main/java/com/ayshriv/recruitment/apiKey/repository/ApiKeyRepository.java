package com.ayshriv.recruitment.apiKey.repository;

import com.ayshriv.recruitment.apiKey.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Persistence access for {@link ApiKey}.
 */
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    /**
     * Find the single active, non deleted key that starts with the given prefix.
     *
     * @param keyPrefix shortened prefix of the raw key
     * @return matching active key, if any
     */
    Optional<ApiKey> findActiveByKeyPrefix(@Param("keyPrefix") String keyPrefix);

    /**
     * Find every non deleted key matching the prefix, eager loading the owning
     * organization so the filter chain can build the principal without opening
     * a transaction.
     *
     * <p>Returns candidates regardless of the active flag so the caller can
     * produce distinct error codes for inactive versus deleted keys.</p>
     *
     * @param keyPrefix shortened prefix of the raw key
     * @return candidate keys
     */
    @Query("""
            select ak from ApiKey ak
            join fetch ak.organization
            where ak.keyPrefix = :keyPrefix
              and ak.isDeleted = false
            """)
    List<ApiKey> findCandidatesByKeyPrefix(@Param("keyPrefix") String keyPrefix);

    /**
     * List all non deleted keys owned by an organization.
     *
     * @param organizationId owning tenant
     * @return non deleted keys of the organization
     */
    @Query("""
            select ak from ApiKey ak
            where ak.organization.id = :organizationId
              and ak.isDeleted = false
            order by ak.createdOn desc
            """)
    List<ApiKey> findAllByOrganizationId(@Param("organizationId") Long organizationId);

    /**
     * Find a non deleted key belonging to an organization.
     *
     * @param id             key primary key
     * @param organizationId owning tenant
     * @return matching key, if any
     */
    @Query("""
            select ak from ApiKey ak
            where ak.id = :id
              and ak.organization.id = :organizationId
              and ak.isDeleted = false
            """)
    Optional<ApiKey> findByIdAndOrganizationId(@Param("id") Long id, @Param("organizationId") Long organizationId);
}