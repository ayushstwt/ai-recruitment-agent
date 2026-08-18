package com.ayshriv.recruitment.job.mapper;

import com.ayshriv.recruitment.client.entity.Client;
import com.ayshriv.recruitment.job.dto.request.CreateJobRequest;
import com.ayshriv.recruitment.job.dto.request.UpdateJobRequest;
import com.ayshriv.recruitment.job.dto.response.JobResponse;
import com.ayshriv.recruitment.job.dto.response.JobSummaryResponse;
import com.ayshriv.recruitment.job.entity.EmploymentType;
import com.ayshriv.recruitment.job.entity.ExperienceLevel;
import com.ayshriv.recruitment.job.entity.Job;
import com.ayshriv.recruitment.job.entity.JobPriority;
import com.ayshriv.recruitment.job.entity.JobStatus;
import com.ayshriv.recruitment.organization.entity.Organization;
import com.ayshriv.recruitment.user.entity.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class JobMapperTest {

    private final JobMapper mapper = new JobMapper();

    private Job job() {
        Client client = new Client();
        client.setId(5L);
        client.setClientCode("CLI-000005");
        client.setCompanyName("Acme Technologies");

        Job job = new Job();
        job.setId(10L);
        job.setJobCode("JOB-000010");
        job.setTitle("Senior Java Developer");
        job.setDescription("Backend development");
        job.setRequirements("Java, Spring Boot");
        job.setResponsibilities("Design APIs");
        job.setLocation("Bengaluru");
        job.setCountry("India");
        job.setState("Karnataka");
        job.setCity("Bengaluru");
        job.setRemote(true);
        job.setEmploymentType(EmploymentType.FULL_TIME);
        job.setExperienceLevel(ExperienceLevel.SENIOR);
        job.setMinExperience(5);
        job.setMaxExperience(9);
        job.setSalaryMin(new BigDecimal("2000000"));
        job.setSalaryMax(new BigDecimal("3500000"));
        job.setCurrency("INR");
        job.setDepartment("Engineering");
        job.setNumberOfOpenings(2);
        job.setStatus(JobStatus.OPEN);
        job.setPriority(JobPriority.HIGH);
        job.setPublishedOn(LocalDateTime.of(2026, 2, 1, 10, 0));
        job.setClosingDate(LocalDate.of(2026, 12, 31));
        job.setOrganization(new Organization(99L));
        job.setClient(client);
        job.setCreatedBy(new User(7L));
        job.setActive(true);
        job.setDeleted(false);
        job.setCreatedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
        job.setUpdatedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
        return job;
    }

    private CreateJobRequest createRequest() {
        return new CreateJobRequest(
                5L, "Senior Java Developer", "Backend development", "Java, Spring Boot",
                "Design APIs", "Bengaluru", "India", "Karnataka", "Bengaluru", true,
                EmploymentType.FULL_TIME, ExperienceLevel.SENIOR, 5, 9,
                new BigDecimal("2000000"), new BigDecimal("3500000"), "INR",
                "Engineering", 2, JobPriority.HIGH, LocalDate.of(2026, 12, 31));
    }

    @Test
    void toResponseMapsAllBusinessFields() {
        JobResponse response = mapper.toResponse(job());

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getJobCode()).isEqualTo("JOB-000010");
        assertThat(response.getTitle()).isEqualTo("Senior Java Developer");
        assertThat(response.getDescription()).isEqualTo("Backend development");
        assertThat(response.getRequirements()).isEqualTo("Java, Spring Boot");
        assertThat(response.getResponsibilities()).isEqualTo("Design APIs");
        assertThat(response.getLocation()).isEqualTo("Bengaluru");
        assertThat(response.getCountry()).isEqualTo("India");
        assertThat(response.getState()).isEqualTo("Karnataka");
        assertThat(response.getCity()).isEqualTo("Bengaluru");
        assertThat(response.isRemote()).isTrue();
        assertThat(response.getEmploymentType()).isEqualTo(EmploymentType.FULL_TIME);
        assertThat(response.getExperienceLevel()).isEqualTo(ExperienceLevel.SENIOR);
        assertThat(response.getMinExperience()).isEqualTo(5);
        assertThat(response.getMaxExperience()).isEqualTo(9);
        assertThat(response.getSalaryMin()).isEqualByComparingTo("2000000");
        assertThat(response.getSalaryMax()).isEqualByComparingTo("3500000");
        assertThat(response.getCurrency()).isEqualTo("INR");
        assertThat(response.getDepartment()).isEqualTo("Engineering");
        assertThat(response.getNumberOfOpenings()).isEqualTo(2);
        assertThat(response.getStatus()).isEqualTo(JobStatus.OPEN);
        assertThat(response.getPriority()).isEqualTo(JobPriority.HIGH);
        assertThat(response.isActive()).isTrue();
        assertThat(response.getClient().getId()).isEqualTo(5L);
        assertThat(response.getClient().getClientCode()).isEqualTo("CLI-000005");
        assertThat(response.getClient().getCompanyName()).isEqualTo("Acme Technologies");
    }

    @Test
    void toResponseNeverExposesEntityOrganizationClientOrCreatedBy() {
        JobResponse response = mapper.toResponse(job());

        assertThat(response).isNotInstanceOf(Job.class);
        assertThat(hasField(response, "organization")).isFalse();
        assertThat(hasField(response, "createdBy")).isFalse();
        assertThat(hasField(response, "isDeleted")).isFalse();
        assertThat(response.getClient()).isInstanceOf(JobResponse.ClientRef.class);
    }

    @Test
    void toSummaryResponseKeepsPayloadLightweight() {
        JobSummaryResponse summary = mapper.toSummaryResponse(job());

        assertThat(summary.getId()).isEqualTo(10L);
        assertThat(summary.getJobCode()).isEqualTo("JOB-000010");
        assertThat(summary.getTitle()).isEqualTo("Senior Java Developer");
        assertThat(summary.getClientId()).isEqualTo(5L);
        assertThat(summary.getClientName()).isEqualTo("Acme Technologies");
        assertThat(summary.getStatus()).isEqualTo(JobStatus.OPEN);
        assertThat(summary.getNumberOfOpenings()).isEqualTo(2);
        assertThat(summary.isActive()).isTrue();
        assertThat(hasField(summary, "description")).isFalse();
        assertThat(hasField(summary, "salaryMin")).isFalse();
        assertThat(hasField(summary, "closingDate")).isFalse();
    }

    @Test
    void toEntityMapsCreationRequestWithoutCodeOrganizationAndStatus() {
        Job entity = mapper.toEntity(createRequest());

        assertThat(entity.getTitle()).isEqualTo("Senior Java Developer");
        assertThat(entity.getDescription()).isEqualTo("Backend development");
        assertThat(entity.getRequirements()).isEqualTo("Java, Spring Boot");
        assertThat(entity.getEmploymentType()).isEqualTo(EmploymentType.FULL_TIME);
        assertThat(entity.getExperienceLevel()).isEqualTo(ExperienceLevel.SENIOR);
        assertThat(entity.getNumberOfOpenings()).isEqualTo(2);
        assertThat(entity.getPriority()).isEqualTo(JobPriority.HIGH);
        assertThat(entity.getJobCode()).isNull();
        assertThat(entity.getOrganization()).isNull();
        assertThat(entity.getClient()).isNull();
        assertThat(entity.getStatus()).isNull();
        assertThat(entity.isActive()).isFalse();
    }

    @Test
    void updateEntityOverwritesOnlyProfileFields() {
        Job job = job();
        UpdateJobRequest request = new UpdateJobRequest(
                "Principal Engineer", "Platform engineering", "Java, Distributed Systems",
                "Own architecture", "Remote", "USA", "California", "San Francisco", false,
                EmploymentType.CONTRACT, ExperienceLevel.LEAD, 10, 15,
                new BigDecimal("150000"), new BigDecimal("220000"), "USD",
                "Platform", 1, JobPriority.URGENT, LocalDate.of(2026, 11, 30));

        mapper.updateEntity(job, request);

        assertThat(job.getTitle()).isEqualTo("Principal Engineer");
        assertThat(job.getEmploymentType()).isEqualTo(EmploymentType.CONTRACT);
        assertThat(job.getExperienceLevel()).isEqualTo(ExperienceLevel.LEAD);
        assertThat(job.getMinExperience()).isEqualTo(10);
        assertThat(job.getMaxExperience()).isEqualTo(15);
        assertThat(job.getSalaryMax()).isEqualByComparingTo("220000");
        assertThat(job.getNumberOfOpenings()).isEqualTo(1);
        assertThat(job.getPriority()).isEqualTo(JobPriority.URGENT);
        assertThat(job.getClosingDate()).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(job.getJobCode()).isEqualTo("JOB-000010");
        assertThat(job.getOrganizationId()).isEqualTo(99L);
        assertThat(job.getClientId()).isEqualTo(5L);
        assertThat(job.getStatus()).isEqualTo(JobStatus.OPEN);
        assertThat(job.isActive()).isTrue();
        assertThat(job.isDeleted()).isFalse();
        assertThat(job.getCreatedOn()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0));
    }

    private boolean hasField(Object object, String fieldName) {
        try {
            object.getClass().getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}
