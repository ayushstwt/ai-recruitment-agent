package com.ayshriv.recruitment.job.repository;

import com.ayshriv.recruitment.job.entity.EmploymentType;
import com.ayshriv.recruitment.job.entity.ExperienceLevel;
import com.ayshriv.recruitment.job.entity.Job;
import com.ayshriv.recruitment.job.entity.JobPriority;
import com.ayshriv.recruitment.job.entity.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Persistence access for {@link Job}.
 *
 * <p>All queries use HQL / JPQL and parameter binding; user input is never
 * concatenated into queries. Every tenant-owned lookup is scoped to an
 * organization id and soft-deleted jobs are never returned, making the owning
 * tenant the hard isolation boundary. Filters assembled from optional request
 * parameters at runtime are handled through {@link JpaSpecificationExecutor}
 * (see the job specification) so the same tenant and deleted guards always
 * apply.</p>
 */
public interface JobRepository extends JpaRepository<Job, Long>, JpaSpecificationExecutor<Job> {

    /**
     * Find a non deleted job by primary key, regardless of its tenant. Used by
     * the service to distinguish a cross-organization job (forbidden) from a
     * missing one (not found).
     *
     * @param id job primary key
     * @return matching job, if any
     */
    @Query("""
            SELECT j
            FROM Job j
            WHERE j.id = :id
              AND j.isDeleted = false
            """)
    Optional<Job> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * Find a non deleted job by primary key within a single organization.
     *
     * @param jobId          job primary key
     * @param organizationId owning tenant
     * @return matching job, if any
     */
    @Query("""
            SELECT j
            FROM Job j
            WHERE j.id = :jobId
              AND j.organization.id = :organizationId
              AND j.isDeleted = false
            """)
    Optional<Job> findByIdAndOrganization(@Param("jobId") Long jobId,
                                          @Param("organizationId") Long organizationId);

    /**
     * Find a non deleted job by job code within a single organization, ignoring
     * case.
     *
     * @param jobCode        human readable job code
     * @param organizationId owning tenant
     * @return matching job, if any
     */
    @Query("""
            SELECT j
            FROM Job j
            WHERE LOWER(j.jobCode) = LOWER(:jobCode)
              AND j.organization.id = :organizationId
              AND j.isDeleted = false
            """)
    Optional<Job> findByJobCodeAndOrganization(@Param("jobCode") String jobCode,
                                               @Param("organizationId") Long organizationId);

    /**
     * Count every job ever created in the organization, including soft deleted
     * ones. Job codes are allocated sequentially and never reused, so this
     * total equals the highest allocated code number and is the base for
     * generating the next code.
     *
     * @param organizationId owning tenant
     * @return total number of job rows in the organization
     */
    @Query("""
            SELECT COUNT(j)
            FROM Job j
            WHERE j.organization.id = :organizationId
            """)
    long countByOrganization(@Param("organizationId") Long organizationId);

    /**
     * Page through all non deleted jobs of an organization, newest first.
     *
     * @param organizationId owning tenant
     * @param pageable       pagination and sorting
     * @return page of non deleted jobs
     */
    @Query("""
            SELECT j
            FROM Job j
            WHERE j.organization.id = :organizationId
              AND j.isDeleted = false
            ORDER BY j.createdOn DESC
            """)
    Page<Job> findJobsByOrganization(@Param("organizationId") Long organizationId, Pageable pageable);

    /**
     * Page through all active, non deleted jobs of an organization.
     *
     * @param organizationId owning tenant
     * @param pageable       pagination and sorting
     * @return page of active jobs
     */
    @Query("""
            SELECT j
            FROM Job j
            WHERE j.organization.id = :organizationId
              AND j.isActive = true
              AND j.isDeleted = false
            ORDER BY j.createdOn DESC
            """)
    Page<Job> findActiveJobs(@Param("organizationId") Long organizationId, Pageable pageable);

    /**
     * Page through all non deleted jobs of a client within an organization.
     *
     * @param clientId       owning client
     * @param organizationId owning tenant
     * @param pageable       pagination and sorting
     * @return page of jobs of the client
     */
    @Query("""
            SELECT j
            FROM Job j
            WHERE j.client.id = :clientId
              AND j.organization.id = :organizationId
              AND j.isDeleted = false
            ORDER BY j.createdOn DESC
            """)
    Page<Job> findJobsByClient(@Param("clientId") Long clientId,
                               @Param("organizationId") Long organizationId,
                               Pageable pageable);

    /**
     * Search non deleted jobs of an organization by keyword across job code,
     * title, description, requirements, location and department, ignoring case.
     *
     * @param organizationId owning tenant
     * @param keyword        search keyword
     * @param pageable       pagination and sorting
     * @return page of matching jobs
     */
    @Query("""
            SELECT j
            FROM Job j
            WHERE j.organization.id = :organizationId
              AND j.isDeleted = false
              AND (
                  LOWER(j.jobCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(j.requirements) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(j.location) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(j.department) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<Job> searchJobs(@Param("organizationId") Long organizationId,
                         @Param("keyword") String keyword, Pageable pageable);

    /**
     * Page through all non deleted jobs of an organization with the given
     * status.
     *
     * @param organizationId owning tenant
     * @param status         target status
     * @param pageable       pagination and sorting
     * @return page of matching jobs
     */
    @Query("""
            SELECT j
            FROM Job j
            WHERE j.organization.id = :organizationId
              AND j.status = :status
              AND j.isDeleted = false
            ORDER BY j.createdOn DESC
            """)
    Page<Job> findJobsByStatus(@Param("organizationId") Long organizationId,
                               @Param("status") JobStatus status, Pageable pageable);

    /**
     * Page through all non deleted jobs of an organization with the given
     * priority.
     *
     * @param organizationId owning tenant
     * @param priority       target priority
     * @param pageable       pagination and sorting
     * @return page of matching jobs
     */
    @Query("""
            SELECT j
            FROM Job j
            WHERE j.organization.id = :organizationId
              AND j.priority = :priority
              AND j.isDeleted = false
            ORDER BY j.createdOn DESC
            """)
    Page<Job> findJobsByPriority(@Param("organizationId") Long organizationId,
                                 @Param("priority") JobPriority priority, Pageable pageable);

    /**
     * Page through all non deleted jobs of an organization with the given
     * employment type.
     *
     * @param organizationId owning tenant
     * @param employmentType target employment type
     * @param pageable       pagination and sorting
     * @return page of matching jobs
     */
    @Query("""
            SELECT j
            FROM Job j
            WHERE j.organization.id = :organizationId
              AND j.employmentType = :employmentType
              AND j.isDeleted = false
            ORDER BY j.createdOn DESC
            """)
    Page<Job> findJobsByEmploymentType(@Param("organizationId") Long organizationId,
                                       @Param("employmentType") EmploymentType employmentType,
                                       Pageable pageable);

    /**
     * Page through all non deleted jobs of an organization with the given
     * experience level.
     *
     * @param organizationId owning tenant
     * @param experienceLevel target experience level
     * @param pageable       pagination and sorting
     * @return page of matching jobs
     */
    @Query("""
            SELECT j
            FROM Job j
            WHERE j.organization.id = :organizationId
              AND j.experienceLevel = :experienceLevel
              AND j.isDeleted = false
            ORDER BY j.createdOn DESC
            """)
    Page<Job> findJobsByExperienceLevel(@Param("organizationId") Long organizationId,
                                        @Param("experienceLevel") ExperienceLevel experienceLevel,
                                        Pageable pageable);
}
