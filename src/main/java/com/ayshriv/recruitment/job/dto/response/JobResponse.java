package com.ayshriv.recruitment.job.dto.response;

import com.ayshriv.recruitment.job.entity.EmploymentType;
import com.ayshriv.recruitment.job.entity.ExperienceLevel;
import com.ayshriv.recruitment.job.entity.JobPriority;
import com.ayshriv.recruitment.job.entity.JobStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Full job representation exposed to API clients.
 *
 * <p>Never exposes the JPA entity, the owning organization entity, the owning
 * client entity or the created-by user entity. The client is represented by a
 * lightweight reference so downstream agents (resume matching, candidate
 * screening) can correlate a job to its client without any entity leakage.</p>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {

    private Long id;

    private String jobCode;

    private String title;

    private String description;

    private String requirements;

    private String responsibilities;

    private String location;

    private String country;

    private String state;

    private String city;

    private boolean remote;

    private EmploymentType employmentType;

    private ExperienceLevel experienceLevel;

    private Integer minExperience;

    private Integer maxExperience;

    private BigDecimal salaryMin;

    private BigDecimal salaryMax;

    private String currency;

    private String department;

    private Integer numberOfOpenings;

    private JobStatus status;

    private JobPriority priority;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "dd-MM-yyyy HH:mm:ss",
            timezone = "Asia/Kolkata"
    )
    private LocalDateTime publishedOn;

    private LocalDate closingDate;

    /**
     * Lightweight client reference. Never a JPA entity.
     */
    private ClientRef client;

    @JsonProperty("isActive")
    private boolean isActive;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "dd-MM-yyyy HH:mm:ss",
            timezone = "Asia/Kolkata"
    )
    private LocalDateTime createdOn;

    @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "dd-MM-yyyy HH:mm:ss",
            timezone = "Asia/Kolkata"
    )
    private LocalDateTime updatedOn;

    /**
     * Summary of the owning client, safe to serialize without the client
     * entity or the tenant.
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClientRef {

        private Long id;

        private String clientCode;

        private String companyName;
    }
}
