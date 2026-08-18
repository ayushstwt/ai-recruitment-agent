package com.ayshriv.recruitment.job.mapper;

import com.ayshriv.recruitment.job.dto.request.CreateJobRequest;
import com.ayshriv.recruitment.job.dto.request.UpdateJobRequest;
import com.ayshriv.recruitment.job.dto.response.JobResponse;
import com.ayshriv.recruitment.job.dto.response.JobSummaryResponse;
import com.ayshriv.recruitment.job.entity.EmploymentType;
import com.ayshriv.recruitment.job.entity.ExperienceLevel;
import com.ayshriv.recruitment.job.entity.Job;
import com.ayshriv.recruitment.job.entity.JobPriority;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Converts between job request / response DTOs and the JPA entity.
 *
 * <p>Response mapping never exposes the owning organization entity, the owning
 * client entity or the created-by user entity; the client is surfaced as a
 * lightweight reference. The job code, the status, the tenant and the
 * lifecycle timestamps are never set from a request: the job code and the
 * status are managed by the service, and the tenant is resolved from the
 * security context.</p>
 */
@Component
public class JobMapper {

    /**
     * Map an entity into its full response representation.
     *
     * @param job source entity
     * @return response DTO
     */
    public JobResponse toResponse(Job job) {
        return JobResponse.builder()
                .id(job.getId())
                .jobCode(job.getJobCode())
                .title(job.getTitle())
                .description(job.getDescription())
                .requirements(job.getRequirements())
                .responsibilities(job.getResponsibilities())
                .location(job.getLocation())
                .country(job.getCountry())
                .state(job.getState())
                .city(job.getCity())
                .remote(job.isRemote())
                .employmentType(job.getEmploymentType())
                .experienceLevel(job.getExperienceLevel())
                .minExperience(job.getMinExperience())
                .maxExperience(job.getMaxExperience())
                .salaryMin(job.getSalaryMin())
                .salaryMax(job.getSalaryMax())
                .currency(job.getCurrency())
                .department(job.getDepartment())
                .numberOfOpenings(job.getNumberOfOpenings())
                .status(job.getStatus())
                .priority(job.getPriority())
                .publishedOn(job.getPublishedOn())
                .closingDate(job.getClosingDate())
                .client(toClientRef(job))
                .isActive(job.isActive())
                .createdOn(job.getCreatedOn())
                .updatedOn(job.getUpdatedOn())
                .build();
    }

    /**
     * Map an entity into its lightweight summary representation.
     *
     * @param job source entity
     * @return summary DTO
     */
    public JobSummaryResponse toSummaryResponse(Job job) {
        return JobSummaryResponse.builder()
                .id(job.getId())
                .jobCode(job.getJobCode())
                .title(job.getTitle())
                .clientId(job.getClientId())
                .clientName(job.getClient() != null ? job.getClient().getCompanyName() : null)
                .location(job.getLocation())
                .remote(job.isRemote())
                .employmentType(job.getEmploymentType())
                .experienceLevel(job.getExperienceLevel())
                .status(job.getStatus())
                .priority(job.getPriority())
                .numberOfOpenings(job.getNumberOfOpenings())
                .isActive(job.isActive())
                .build();
    }

    /**
     * Build a transient entity from a creation request. Only business profile
     * fields are mapped; the job code, the status, the organization and the
     * client are set by the service.
     *
     * @param request creation payload
     * @return transient entity
     */
    public Job toEntity(CreateJobRequest request) {
        Job job = new Job();
        applyProfileFields(job, request.getTitle(), request.getDescription(), request.getRequirements(),
                request.getResponsibilities(), request.getLocation(), request.getCountry(), request.getState(),
                request.getCity(), request.getRemote(), request.getEmploymentType(), request.getExperienceLevel(),
                request.getMinExperience(), request.getMaxExperience(), request.getSalaryMin(), request.getSalaryMax(),
                request.getCurrency(), request.getDepartment(), request.getNumberOfOpenings(), request.getPriority(),
                request.getClosingDate());
        return job;
    }

    /**
     * Apply updatable profile fields from an update request onto an existing
     * entity. Immutable fields (id, job code, organization, client, status,
     * timestamps, active, deleted) are never touched.
     *
     * @param job     target entity
     * @param request update payload
     */
    public void updateEntity(Job job, UpdateJobRequest request) {
        applyProfileFields(job, request.getTitle(), request.getDescription(), request.getRequirements(),
                request.getResponsibilities(), request.getLocation(), request.getCountry(), request.getState(),
                request.getCity(), request.getRemote(), request.getEmploymentType(), request.getExperienceLevel(),
                request.getMinExperience(), request.getMaxExperience(), request.getSalaryMin(), request.getSalaryMax(),
                request.getCurrency(), request.getDepartment(), request.getNumberOfOpenings(), request.getPriority(),
                request.getClosingDate());
    }

    /**
     * Build the lightweight client reference of a job.
     *
     * @param job source entity
     * @return client reference, or {@code null} when the job has no client
     */
    private JobResponse.ClientRef toClientRef(Job job) {
        if (job.getClient() == null) {
            return null;
        }
        return JobResponse.ClientRef.builder()
                .id(job.getClient().getId())
                .clientCode(job.getClient().getClientCode())
                .companyName(job.getClient().getCompanyName())
                .build();
    }

    /**
     * Copy the shared profile fields onto an entity.
     */
    private void applyProfileFields(Job job, String title, String description, String requirements,
                                    String responsibilities, String location, String country, String state,
                                    String city, Boolean remote, EmploymentType employmentType,
                                    ExperienceLevel experienceLevel, Integer minExperience, Integer maxExperience,
                                    BigDecimal salaryMin, BigDecimal salaryMax, String currency, String department,
                                    Integer numberOfOpenings, JobPriority priority, LocalDate closingDate) {
        job.setTitle(title);
        job.setDescription(description);
        job.setRequirements(requirements);
        job.setResponsibilities(responsibilities);
        job.setLocation(location);
        job.setCountry(country);
        job.setState(state);
        job.setCity(city);
        job.setRemote(remote != null && remote);
        job.setEmploymentType(employmentType);
        job.setExperienceLevel(experienceLevel);
        job.setMinExperience(minExperience);
        job.setMaxExperience(maxExperience);
        job.setSalaryMin(salaryMin);
        job.setSalaryMax(salaryMax);
        job.setCurrency(currency);
        job.setDepartment(department);
        job.setNumberOfOpenings(numberOfOpenings);
        job.setPriority(priority);
        job.setClosingDate(closingDate);
    }
}
