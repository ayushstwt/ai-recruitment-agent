package com.ayshriv.recruitment.job.specification;

import com.ayshriv.recruitment.job.entity.EmploymentType;
import com.ayshriv.recruitment.job.entity.ExperienceLevel;
import com.ayshriv.recruitment.job.entity.Job;
import com.ayshriv.recruitment.job.entity.JobPriority;
import com.ayshriv.recruitment.job.entity.JobStatus;
import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable JPA specifications for {@link Job}.
 *
 * <p>Complements the HQL / JPQL queries in {@code JobRepository} for the cases
 * where a query is assembled from optional filters at runtime. Every
 * specification is tenant-aware: no job can ever be selected without its owning
 * organization id, keeping the hard isolation boundary intact.</p>
 */
public final class JobSpecification {

    private JobSpecification() {
    }

    /**
     * Job belonging to the given organization.
     *
     * @param organizationId owning tenant
     * @return specification
     */
    public static Specification<Job> belongsToOrganization(Long organizationId) {
        return (root, query, builder) -> builder.equal(
                root.get("organization").get("id"), organizationId);
    }

    /**
     * Job that has not been soft deleted.
     *
     * @return specification
     */
    public static Specification<Job> notDeleted() {
        return (root, query, builder) -> builder.equal(root.get("isDeleted"), false);
    }

    /**
     * Job that is currently active.
     *
     * @return specification
     */
    public static Specification<Job> active() {
        return (root, query, builder) -> builder.equal(root.get("isActive"), true);
    }

    /**
     * Job belonging to the given client.
     *
     * @param clientId owning client
     * @return specification
     */
    public static Specification<Job> belongsToClient(Long clientId) {
        return (root, query, builder) -> builder.equal(root.get("client").get("id"), clientId);
    }

    /**
     * Job with the given status.
     *
     * @param status target status
     * @return specification
     */
    public static Specification<Job> status(JobStatus status) {
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    /**
     * Job with the given priority.
     *
     * @param priority target priority
     * @return specification
     */
    public static Specification<Job> priority(JobPriority priority) {
        return (root, query, builder) -> builder.equal(root.get("priority"), priority);
    }

    /**
     * Job with the given employment type.
     *
     * @param employmentType target employment type
     * @return specification
     */
    public static Specification<Job> employmentType(EmploymentType employmentType) {
        return (root, query, builder) -> builder.equal(root.get("employmentType"), employmentType);
    }

    /**
     * Job with the given experience level.
     *
     * @param experienceLevel target experience level
     * @return specification
     */
    public static Specification<Job> experienceLevel(ExperienceLevel experienceLevel) {
        return (root, query, builder) -> builder.equal(root.get("experienceLevel"), experienceLevel);
    }

    /**
     * Job whose job code, title, description, requirements, location or
     * department contains the keyword, ignoring case.
     *
     * @param keyword search keyword, never blank
     * @return specification
     */
    public static Specification<Job> keyword(String keyword) {
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, builder) -> {
            var jobCode = builder.lower(root.get("jobCode"));
            var title = builder.lower(root.get("title"));
            var description = builder.lower(root.get("description"));
            var requirements = builder.lower(root.get("requirements"));
            var location = builder.lower(root.get("location"));
            var department = builder.lower(root.get("department"));
            return builder.or(
                    builder.like(jobCode, pattern),
                    builder.like(title, pattern),
                    builder.like(description, pattern),
                    builder.like(requirements, pattern),
                    builder.like(location, pattern),
                    builder.like(department, pattern));
        };
    }
}
