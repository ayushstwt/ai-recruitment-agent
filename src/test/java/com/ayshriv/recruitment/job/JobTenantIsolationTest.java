package com.ayshriv.recruitment.job;

import com.ayshriv.recruitment.apiKey.security.ApiKeyAuthenticationEntryPoint;
import com.ayshriv.recruitment.apiKey.security.ApiKeyPrincipal;
import com.ayshriv.recruitment.apiKey.service.ApiKeyService;
import com.ayshriv.recruitment.client.entity.Client;
import com.ayshriv.recruitment.client.repository.ClientRepository;
import com.ayshriv.recruitment.common.config.AppProperties;
import com.ayshriv.recruitment.common.security.SecurityConfig;
import com.ayshriv.recruitment.common.security.SecurityContextService;
import com.ayshriv.recruitment.job.controller.JobClientController;
import com.ayshriv.recruitment.job.controller.JobController;
import com.ayshriv.recruitment.job.entity.EmploymentType;
import com.ayshriv.recruitment.job.entity.ExperienceLevel;
import com.ayshriv.recruitment.job.entity.Job;
import com.ayshriv.recruitment.job.entity.JobPriority;
import com.ayshriv.recruitment.job.entity.JobStatus;
import com.ayshriv.recruitment.job.mapper.JobMapper;
import com.ayshriv.recruitment.job.repository.JobRepository;
import com.ayshriv.recruitment.job.service.JobCodeGenerator;
import com.ayshriv.recruitment.job.service.JobService;
import com.ayshriv.recruitment.job.service.JobStatusTransitionService;
import com.ayshriv.recruitment.organization.entity.Organization;
import com.ayshriv.recruitment.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mandatory tenant isolation test for jobs.
 *
 * <p>Organization A and Organization B exist. Job B belongs to organization B
 * and its client belongs to organization B. API key A authenticates as
 * organization A. Every attempt to reach job B through an id based endpoint
 * must fail with {@code 403 FORBIDDEN} and the {@code JOB_ACCESS_DENIED} error
 * code.</p>
 *
 * <p>The real {@link JobService} is wired into the web slice so the isolation
 * decision is produced by the actual service logic, not by a mock.</p>
 */
@WebMvcTest(controllers = {JobController.class, JobClientController.class})
@AutoConfigureMockMvc
@Import({SecurityConfig.class, ApiKeyAuthenticationEntryPoint.class, SecurityContextService.class,
        AppProperties.class, JobService.class, JobMapper.class, JobCodeGenerator.class,
        JobStatusTransitionService.class})
class JobTenantIsolationTest {

    private static final String API_KEY_A = "test-api-key-org-a";

    private static final String UPDATE_BODY = """
            {
                "title": "Principal Engineer",
                "description": "Platform engineering",
                "requirements": "Java, Distributed Systems",
                "employmentType": "CONTRACT",
                "experienceLevel": "LEAD",
                "numberOfOpenings": 1
            }
            """;

    private static final String STATUS_BODY = """
            {
                "status": "OPEN"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @MockitoBean
    private JobRepository jobRepository;

    @MockitoBean
    private ClientRepository clientRepository;

    @MockitoBean
    private OrganizationRepository organizationRepository;

    @Test
    void organizationACannotGetJobB() throws Exception {
        mockAuthentication();
        mockJobB();

        mockMvc.perform(get("/api/v1/jobs/2").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("JOB_ACCESS_DENIED"));
    }

    @Test
    void organizationACannotUpdateJobB() throws Exception {
        mockAuthentication();
        mockJobB();

        mockMvc.perform(put("/api/v1/jobs/2")
                        .header("X-API-KEY", API_KEY_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("JOB_ACCESS_DENIED"));
    }

    @Test
    void organizationACannotDeleteJobB() throws Exception {
        mockAuthentication();
        mockJobB();

        mockMvc.perform(delete("/api/v1/jobs/2").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("JOB_ACCESS_DENIED"));
    }

    @Test
    void organizationACannotChangeStatusOfJobB() throws Exception {
        mockAuthentication();
        mockJobB();

        mockMvc.perform(patch("/api/v1/jobs/2/status")
                        .header("X-API-KEY", API_KEY_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(STATUS_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("JOB_ACCESS_DENIED"));
    }

    @Test
    void organizationACannotPublishJobB() throws Exception {
        mockAuthentication();
        mockJobB();

        mockMvc.perform(patch("/api/v1/jobs/2/publish").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("JOB_ACCESS_DENIED"));
    }

    @Test
    void organizationACannotHoldJobB() throws Exception {
        mockAuthentication();
        mockJobB();

        mockMvc.perform(patch("/api/v1/jobs/2/hold").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("JOB_ACCESS_DENIED"));
    }

    @Test
    void organizationACannotCloseJobB() throws Exception {
        mockAuthentication();
        mockJobB();

        mockMvc.perform(patch("/api/v1/jobs/2/close").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("JOB_ACCESS_DENIED"));
    }

    @Test
    void organizationACannotCancelJobB() throws Exception {
        mockAuthentication();
        mockJobB();

        mockMvc.perform(patch("/api/v1/jobs/2/cancel").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("JOB_ACCESS_DENIED"));
    }

    @Test
    void organizationACannotChangeClientOfJobB() throws Exception {
        mockAuthentication();
        mockJobB();

        mockMvc.perform(patch("/api/v1/jobs/2/client")
                        .header("X-API-KEY", API_KEY_A)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":6}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("JOB_ACCESS_DENIED"));
    }

    @Test
    void organizationACanAccessItsOwnJob() throws Exception {
        mockAuthentication();
        when(jobRepository.findByIdAndNotDeleted(1L)).thenReturn(Optional.of(job(1L, 1L)));

        mockMvc.perform(get("/api/v1/jobs/1").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.jobCode").value("JOB-000001"))
                .andExpect(jsonPath("$.data.title").value("Senior Java Developer"))
                .andExpect(jsonPath("$.data.client.companyName").value("Acme Technologies"));
    }

    @Test
    void organizationACanListOwnClientJobs() throws Exception {
        mockAuthentication();
        when(clientRepository.findByIdAndOrganization(5L, 1L))
                .thenReturn(Optional.of(client(5L, 1L)));
        when(jobRepository.findJobsByClient(eq(5L), eq(1L), any()))
                .thenReturn(new PageImpl<>(List.of(job(1L, 1L)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/clients/5/jobs").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].client.id").value(5));
    }

    @Test
    void missingJobReturns404() throws Exception {
        mockAuthentication();
        when(jobRepository.findByIdAndNotDeleted(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/jobs/99").header("X-API-KEY", API_KEY_A))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("JOB_NOT_FOUND"));
    }

    private void mockAuthentication() {
        when(apiKeyService.authenticate(API_KEY_A)).thenReturn(new UsernamePasswordAuthenticationToken(
                new ApiKeyPrincipal(1L, 1L, "org-a-key"), null,
                List.of(new SimpleGrantedAuthority("ROLE_API_KEY"))));
    }

    private void mockJobB() {
        when(jobRepository.findByIdAndNotDeleted(2L)).thenReturn(Optional.of(job(2L, 2L)));
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

    private Job job(Long id, Long organizationId) {
        Job job = new Job();
        job.setId(id);
        job.setJobCode("JOB-" + String.format("%06d", id));
        job.setTitle("Senior Java Developer");
        job.setDescription("Backend development");
        job.setRequirements("Java, Spring Boot");
        job.setEmploymentType(EmploymentType.FULL_TIME);
        job.setExperienceLevel(ExperienceLevel.SENIOR);
        job.setNumberOfOpenings(2);
        job.setStatus(JobStatus.OPEN);
        job.setPriority(JobPriority.HIGH);
        job.setOrganization(new Organization(organizationId));
        job.setClient(client(id + 4L, organizationId));
        job.setActive(true);
        job.setDeleted(false);
        job.setCreatedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
        job.setUpdatedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
        return job;
    }
}
