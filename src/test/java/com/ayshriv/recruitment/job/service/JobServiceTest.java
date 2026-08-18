package com.ayshriv.recruitment.job.service;

import com.ayshriv.recruitment.client.entity.Client;
import com.ayshriv.recruitment.client.repository.ClientRepository;
import com.ayshriv.recruitment.common.exception.BadRequestException;
import com.ayshriv.recruitment.common.security.SecurityContextService;
import com.ayshriv.recruitment.job.dto.request.ChangeJobClientRequest;
import com.ayshriv.recruitment.job.dto.request.CreateJobRequest;
import com.ayshriv.recruitment.job.dto.request.UpdateJobRequest;
import com.ayshriv.recruitment.job.dto.request.UpdateJobStatusRequest;
import com.ayshriv.recruitment.job.dto.response.JobResponse;
import com.ayshriv.recruitment.job.entity.EmploymentType;
import com.ayshriv.recruitment.job.entity.ExperienceLevel;
import com.ayshriv.recruitment.job.entity.Job;
import com.ayshriv.recruitment.job.entity.JobPriority;
import com.ayshriv.recruitment.job.entity.JobStatus;
import com.ayshriv.recruitment.job.exception.JobAccessDeniedException;
import com.ayshriv.recruitment.job.exception.JobNotFoundException;
import com.ayshriv.recruitment.job.mapper.JobMapper;
import com.ayshriv.recruitment.job.repository.JobRepository;
import com.ayshriv.recruitment.organization.entity.Organization;
import com.ayshriv.recruitment.organization.exception.OrganizationNotFoundException;
import com.ayshriv.recruitment.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private JobCodeGenerator jobCodeGenerator;

    @Mock
    private JobStatusTransitionService jobStatusTransitionService;

    @Mock
    private SecurityContextService securityContextService;

    @Spy
    private JobMapper jobMapper = new JobMapper();

    private JobService jobService;

    @BeforeEach
    void setUp() {
        jobService = new JobService(jobRepository, clientRepository, organizationRepository, jobMapper,
                jobCodeGenerator, jobStatusTransitionService, securityContextService);
    }

    private Client client(Long id, Long organizationId) {
        Client client = new Client();
        client.setId(id);
        client.setClientCode("CLI-" + String.format("%06d", id));
        client.setCompanyName("Acme Technologies");
        client.setOrganization(new Organization(organizationId));
        client.setActive(true);
        client.setDeleted(false);
        return client;
    }

    private Job job(Long id, Long organizationId, Long clientId, JobStatus status) {
        Job job = new Job();
        job.setId(id);
        job.setJobCode("JOB-" + String.format("%06d", id));
        job.setTitle("Senior Java Developer");
        job.setDescription("Backend development");
        job.setRequirements("Java, Spring Boot");
        job.setEmploymentType(EmploymentType.FULL_TIME);
        job.setExperienceLevel(ExperienceLevel.SENIOR);
        job.setNumberOfOpenings(2);
        job.setStatus(status);
        job.setPriority(JobPriority.HIGH);
        job.setOrganization(new Organization(organizationId));
        job.setClient(client(clientId, organizationId));
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

    private UpdateJobRequest updateRequest() {
        return new UpdateJobRequest(
                "Principal Engineer", "Platform engineering", "Java, Distributed Systems",
                "Own architecture", "Remote", "USA", "California", "San Francisco", false,
                EmploymentType.CONTRACT, ExperienceLevel.LEAD, 10, 15,
                new BigDecimal("150000"), new BigDecimal("220000"), "USD",
                "Platform", 1, JobPriority.URGENT, LocalDate.of(2026, 11, 30));
    }

    @Test
    void createJobAssignsOrganizationClientCodeAndDraftStatus() {
        when(clientRepository.findByIdAndOrganization(5L, 10L))
                .thenReturn(Optional.of(client(5L, 10L)));
        when(organizationRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(new Organization(10L)));
        when(jobCodeGenerator.nextCode(10L)).thenReturn("JOB-000001");
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobResponse response = jobService.createJob(10L, createRequest());

        ArgumentCaptor<Job> captor = ArgumentCaptor.forClass(Job.class);
        verify(jobRepository).save(captor.capture());
        Job saved = captor.getValue();

        assertThat(response.getJobCode()).isEqualTo("JOB-000001");
        assertThat(saved.getJobCode()).isEqualTo("JOB-000001");
        assertThat(saved.getOrganizationId()).isEqualTo(10L);
        assertThat(saved.getClientId()).isEqualTo(5L);
        assertThat(saved.getStatus()).isEqualTo(JobStatus.DRAFT);
        assertThat(saved.getTitle()).isEqualTo("Senior Java Developer");
        assertThat(saved.getPriority()).isEqualTo(JobPriority.HIGH);
        verify(organizationRepository).findByIdForUpdate(10L);
    }

    @Test
    void createJobLocksTheOrganizationBeforeCodeAllocation() {
        when(clientRepository.findByIdAndOrganization(5L, 10L))
                .thenReturn(Optional.of(client(5L, 10L)));
        when(organizationRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.createJob(10L, createRequest()))
                .isInstanceOf(OrganizationNotFoundException.class);
        verify(jobRepository, never()).save(any());
        verify(jobCodeGenerator, never()).nextCode(10L);
    }

    @Test
    void createJobRejectsClientOfAnotherOrganization() {
        when(clientRepository.findByIdAndOrganization(5L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.createJob(10L, createRequest()))
                .isInstanceOf(JobNotFoundException.class)
                .extracting(ex -> ((JobNotFoundException) ex).getCode())
                .isEqualTo("JOB_NOT_FOUND");
        verify(jobRepository, never()).save(any());
    }

    @Test
    void createJobRejectsMinExperienceGreaterThanMax() {
        CreateJobRequest request = createRequest();
        request.setMinExperience(10);
        request.setMaxExperience(5);

        assertThatThrownBy(() -> jobService.createJob(10L, request))
                .isInstanceOf(BadRequestException.class)
                .extracting(ex -> ((BadRequestException) ex).getCode())
                .isEqualTo("INVALID_EXPERIENCE_RANGE");
        verify(jobRepository, never()).save(any());
    }

    @Test
    void createJobRejectsSalaryMinGreaterThanMax() {
        CreateJobRequest request = createRequest();
        request.setSalaryMin(new BigDecimal("3500000"));
        request.setSalaryMax(new BigDecimal("2000000"));

        assertThatThrownBy(() -> jobService.createJob(10L, request))
                .isInstanceOf(BadRequestException.class)
                .extracting(ex -> ((BadRequestException) ex).getCode())
                .isEqualTo("INVALID_SALARY_RANGE");
        verify(jobRepository, never()).save(any());
    }

    @Test
    void createJobResponseNeverExposesEntityOrOrganization() {
        when(clientRepository.findByIdAndOrganization(5L, 10L))
                .thenReturn(Optional.of(client(5L, 10L)));
        when(organizationRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(new Organization(10L)));
        when(jobCodeGenerator.nextCode(10L)).thenReturn("JOB-000001");
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobResponse response = jobService.createJob(10L, createRequest());

        assertThat(response).isNotInstanceOf(Job.class);
        assertThat(hasField(response, "organization")).isFalse();
        assertThat(hasField(response, "createdBy")).isFalse();
        assertThat(hasField(response, "isDeleted")).isFalse();
    }

    @Test
    void getJobByIdReturnsMappedJob() {
        when(jobRepository.findByIdAndNotDeleted(10L))
                .thenReturn(Optional.of(job(10L, 10L, 5L, JobStatus.OPEN)));

        JobResponse response = jobService.getJobById(10L, 10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getJobCode()).isEqualTo("JOB-000010");
        assertThat(response.getTitle()).isEqualTo("Senior Java Developer");
        assertThat(response.getStatus()).isEqualTo(JobStatus.OPEN);
        assertThat(response.isActive()).isTrue();
        assertThat(response.getClient().getId()).isEqualTo(5L);
    }

    @Test
    void getJobByIdThrowsNotFound() {
        when(jobRepository.findByIdAndNotDeleted(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getJobById(10L, 99L))
                .isInstanceOf(JobNotFoundException.class)
                .extracting(ex -> ((JobNotFoundException) ex).getCode())
                .isEqualTo("JOB_NOT_FOUND");
    }

    @Test
    void getJobByIdThrowsForbiddenWhenJobBelongsToAnotherOrganization() {
        when(jobRepository.findByIdAndNotDeleted(2L))
                .thenReturn(Optional.of(job(2L, 20L, 5L, JobStatus.OPEN)));

        assertThatThrownBy(() -> jobService.getJobById(10L, 2L))
                .isInstanceOf(JobAccessDeniedException.class)
                .extracting(ex -> ((JobAccessDeniedException) ex).getCode())
                .isEqualTo("JOB_ACCESS_DENIED");
    }

    @Test
    void getJobsDelegatesToTenantScopedSpecification() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Job> page = new PageImpl<>(List.of(job(10L, 10L, 5L, JobStatus.OPEN)), pageable, 1);
        when(jobRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(page);

        Page<JobResponse> result =
                jobService.getJobs(10L, JobStatus.OPEN, JobPriority.HIGH, 5L,
                        EmploymentType.FULL_TIME, ExperienceLevel.SENIOR, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getJobCode()).isEqualTo("JOB-000010");
        verify(jobRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
    }

    @Test
    void getJobsWithoutFiltersStillScopesToOrganization() {
        Pageable pageable = PageRequest.of(0, 20);
        when(jobRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        jobService.getJobs(10L, null, null, null, null, null, pageable);

        verify(jobRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
    }

    @Test
    void searchJobsDelegatesToTenantScopedSpecification() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Job> page = new PageImpl<>(List.of(job(10L, 10L, 5L, JobStatus.OPEN)), pageable, 1);
        when(jobRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable)))
                .thenReturn(page);

        Page<com.ayshriv.recruitment.job.dto.response.JobSummaryResponse> result =
                jobService.searchJobs(10L, "java", 5L, JobStatus.OPEN, JobPriority.HIGH, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getClientId()).isEqualTo(5L);
        verify(jobRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class), eq(pageable));
    }

    @Test
    void getJobsByClientVerifiesClientAndDelegatesToOrganizationScopedQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Job> page = new PageImpl<>(List.of(job(10L, 10L, 5L, JobStatus.OPEN)), pageable, 1);
        when(clientRepository.findByIdAndOrganization(5L, 10L))
                .thenReturn(Optional.of(client(5L, 10L)));
        when(jobRepository.findJobsByClient(5L, 10L, pageable)).thenReturn(page);

        Page<JobResponse> result = jobService.getJobsByClient(10L, 5L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getClient().getId()).isEqualTo(5L);
        verify(jobRepository).findJobsByClient(5L, 10L, pageable);
    }

    @Test
    void getJobsByClientRejectsClientOfAnotherOrganization() {
        when(clientRepository.findByIdAndOrganization(5L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getJobsByClient(10L, 5L, PageRequest.of(0, 20)))
                .isInstanceOf(JobNotFoundException.class);
    }

    @Test
    void updateJobAppliesProfileChanges() {
        Job job = job(10L, 10L, 5L, JobStatus.OPEN);
        when(jobRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobResponse response = jobService.updateJob(10L, 10L, updateRequest());

        assertThat(response.getTitle()).isEqualTo("Principal Engineer");
        assertThat(response.getEmploymentType()).isEqualTo(EmploymentType.CONTRACT);
        assertThat(response.getExperienceLevel()).isEqualTo(ExperienceLevel.LEAD);
        assertThat(response.getJobCode()).isEqualTo("JOB-000010");
        assertThat(job.getClientId()).isEqualTo(5L);
        assertThat(job.getStatus()).isEqualTo(JobStatus.OPEN);
        assertThat(job.isActive()).isTrue();
        assertThat(job.isDeleted()).isFalse();
    }

    @Test
    void updateJobRejectsInvalidSalaryRange() {
        UpdateJobRequest request = updateRequest();
        request.setSalaryMin(new BigDecimal("220000"));
        request.setSalaryMax(new BigDecimal("150000"));

        assertThatThrownBy(() -> jobService.updateJob(10L, 10L, request))
                .isInstanceOf(BadRequestException.class)
                .extracting(ex -> ((BadRequestException) ex).getCode())
                .isEqualTo("INVALID_SALARY_RANGE");
        verify(jobRepository, never()).save(any());
    }

    @Test
    void updateJobThrowsForbiddenWhenTargetBelongsToAnotherOrganization() {
        when(jobRepository.findByIdAndNotDeleted(2L))
                .thenReturn(Optional.of(job(2L, 20L, 5L, JobStatus.OPEN)));

        assertThatThrownBy(() -> jobService.updateJob(10L, 2L, updateRequest()))
                .isInstanceOf(JobAccessDeniedException.class)
                .extracting(ex -> ((JobAccessDeniedException) ex).getCode())
                .isEqualTo("JOB_ACCESS_DENIED");
    }

    @Test
    void changeJobClientReassignsWithinSameOrganization() {
        Job job = job(10L, 10L, 5L, JobStatus.OPEN);
        when(jobRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(job));
        when(clientRepository.findByIdAndOrganization(6L, 10L))
                .thenReturn(Optional.of(client(6L, 10L)));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobResponse response =
                jobService.changeJobClient(10L, 10L, new ChangeJobClientRequest(6L));

        assertThat(response.getClient().getId()).isEqualTo(6L);
        assertThat(job.getClientId()).isEqualTo(6L);
        assertThat(job.getOrganizationId()).isEqualTo(10L);
    }

    @Test
    void changeJobClientRejectsClientOfAnotherOrganization() {
        when(jobRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(job(10L, 10L, 5L, JobStatus.OPEN)));
        when(clientRepository.findByIdAndOrganization(6L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.changeJobClient(10L, 10L, new ChangeJobClientRequest(6L)))
                .isInstanceOf(JobNotFoundException.class);
        verify(jobRepository, never()).save(any());
    }

    @Test
    void updateJobStatusValidatesTransitionBeforeSaving() {
        Job job = job(10L, 10L, 5L, JobStatus.DRAFT);
        when(jobRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        jobService.updateJobStatus(10L, 10L, new UpdateJobStatusRequest(JobStatus.OPEN));

        verify(jobStatusTransitionService).apply(job, JobStatus.OPEN);
        verify(jobRepository).save(job);
    }

    @Test
    void publishJobMovesDraftToOpen() {
        Job job = job(10L, 10L, 5L, JobStatus.DRAFT);
        when(jobRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobResponse response = jobService.publishJob(10L, 10L);

        verify(jobStatusTransitionService).apply(job, JobStatus.OPEN);
        assertThat(response.getStatus()).isEqualTo(JobStatus.DRAFT);
    }

    @Test
    void putJobOnHoldMovesOpenToOnHold() {
        Job job = job(10L, 10L, 5L, JobStatus.OPEN);
        when(jobRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        jobService.putJobOnHold(10L, 10L);

        verify(jobStatusTransitionService).apply(job, JobStatus.ON_HOLD);
    }

    @Test
    void closeJobMovesOpenToClosed() {
        Job job = job(10L, 10L, 5L, JobStatus.OPEN);
        when(jobRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        jobService.closeJob(10L, 10L);

        verify(jobStatusTransitionService).apply(job, JobStatus.CLOSED);
    }

    @Test
    void cancelJobMovesDraftToCancelled() {
        Job job = job(10L, 10L, 5L, JobStatus.DRAFT);
        when(jobRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        jobService.cancelJob(10L, 10L);

        verify(jobStatusTransitionService).apply(job, JobStatus.CANCELLED);
    }

    @Test
    void statusActionsThrowForbiddenForAnotherOrganization() {
        when(jobRepository.findByIdAndNotDeleted(2L))
                .thenReturn(Optional.of(job(2L, 20L, 5L, JobStatus.OPEN)));

        assertThatThrownBy(() -> jobService.publishJob(10L, 2L))
                .isInstanceOf(JobAccessDeniedException.class);
        verify(jobStatusTransitionService, never()).apply(any(), any());
    }

    @Test
    void activateJobActivatesWithoutDeleting() {
        Job job = job(10L, 10L, 5L, JobStatus.OPEN);
        job.setActive(false);
        when(jobRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobResponse response = jobService.activateJob(10L, 10L);

        assertThat(response.isActive()).isTrue();
        assertThat(job.isActive()).isTrue();
        assertThat(job.isDeleted()).isFalse();
    }

    @Test
    void activateJobRejectsClosedOrCancelledJob() {
        Job job = job(10L, 10L, 5L, JobStatus.CLOSED);
        job.setActive(false);
        when(jobRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.activateJob(10L, 10L))
                .isInstanceOf(BadRequestException.class)
                .extracting(ex -> ((BadRequestException) ex).getCode())
                .isEqualTo("JOB_CANNOT_BE_ACTIVATED");
        verify(jobRepository, never()).save(any());
    }

    @Test
    void deactivateJobDeactivatesWithoutDeleting() {
        Job job = job(10L, 10L, 5L, JobStatus.OPEN);
        when(jobRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        JobResponse response = jobService.deactivateJob(10L, 10L);

        assertThat(response.isActive()).isFalse();
        assertThat(job.isDeleted()).isFalse();
    }

    @Test
    void deleteJobSoftDeletes() {
        Job job = job(10L, 10L, 5L, JobStatus.OPEN);
        when(jobRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(job));
        when(jobRepository.save(any(Job.class))).thenAnswer(invocation -> invocation.getArgument(0));

        jobService.deleteJob(10L, 10L);

        assertThat(job.isDeleted()).isTrue();
        assertThat(job.isActive()).isFalse();
        verify(jobRepository).save(job);
        verify(jobRepository, never()).delete(any(Job.class));
    }

    @Test
    void deleteJobThrowsForbiddenForAnotherOrganization() {
        when(jobRepository.findByIdAndNotDeleted(2L))
                .thenReturn(Optional.of(job(2L, 20L, 5L, JobStatus.OPEN)));

        assertThatThrownBy(() -> jobService.deleteJob(10L, 2L))
                .isInstanceOf(JobAccessDeniedException.class)
                .extracting(ex -> ((JobAccessDeniedException) ex).getCode())
                .isEqualTo("JOB_ACCESS_DENIED");
        verify(jobRepository, never()).save(any());
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
