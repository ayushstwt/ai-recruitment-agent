package com.ayshriv.recruitment.job.dto.response;

import com.ayshriv.recruitment.job.entity.EmploymentType;
import com.ayshriv.recruitment.job.entity.ExperienceLevel;
import com.ayshriv.recruitment.job.entity.JobPriority;
import com.ayshriv.recruitment.job.entity.JobStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Lightweight job representation used for search results, dropdowns, dashboards
 * and candidate matching.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobSummaryResponse {

    private Long id;

    private String jobCode;

    private String title;

    private Long clientId;

    private String clientName;

    private String location;

    private boolean remote;

    private EmploymentType employmentType;

    private ExperienceLevel experienceLevel;

    private JobStatus status;

    private JobPriority priority;

    private Integer numberOfOpenings;

    @JsonProperty("isActive")
    private boolean isActive;
}
