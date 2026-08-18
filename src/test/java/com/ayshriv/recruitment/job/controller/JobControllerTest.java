package com.ayshriv.recruitment.job.controller;

import com.ayshriv.recruitment.apiKey.security.ApiKeyAuthenticationEntryPoint;
import com.ayshriv.recruitment.apiKey.security.ApiKeyPrincipal;
import com.ayshriv.recruitment.apiKey.service.ApiKeyService;
import com.ayshriv.recruitment.common.config.AppProperties;
import com.ayshriv.recruitment.common.security.SecurityConfig;
import com.ayshriv.recruitment.common.security.SecurityContextService;
import com.ayshriv.recruitment.job.dto.response.JobResponse;
import com.ayshriv.recruitment.job.dto.response.JobSummaryResponse;
import com.ayshriv.recruitment.job.entity.EmploymentType;
import com.ayshriv.recruitment.job.entity.ExperienceLevel;
import com.ayshriv.recruitment.job.entity.JobPriority;
import com.ayshriv.recruitment.job.entity.JobStatus;
import com.ayshriv.recruitment.job.exception.JobAccessDeniedException;
import com.ayshriv.recruitment.job.exception.JobNotFoundException;
import com.ayshriv.recruitment.job.service.JobService;
import org.junit.jupiter.api.BeforeEach;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {JobController.class, JobClientController.class})
@AutoConfigureMockMvc
@Import({SecurityConfig.class, ApiKeyAuthenticationEntryPoint.class, SecurityContextService.class,
        AppProperties.class})
class JobControllerTest {

    private static final String API_KEY = "test-api-key";

    private static final String CREATE_BODY = """
            {
                "clientId": 5,
                "title": "Senior Java Developer",
                "description": "Backend development",
                "requirements": "Java, Spring Boot",
                "responsibilities": "Design APIs",
                "location": "Bengaluru",
                "country": "India",
                "state": "Karnataka",
                "city": "Bengaluru",
                "remote": true,
                "employmentType": "FULL_TIME",
                "experienceLevel": "SENIOR",
                "minExperience": 5,
                "maxExperience": 9,
                "salaryMin": 2000000,
                "salaryMax": 3500000,
                "currency": "INR",
                "department": "Engineering",
                "numberOfOpenings": 2,
                "priority": "HIGH",
                "closingDate": "2026-12-31"
            }
            """;

    private static final String UPDATE_BODY = """
            {
                "title": "Principal Engineer",
                "description": "Platform engineering",
                "requirements": "Java, Distributed Systems",
                "responsibilities": "Own architecture",
                "location": "Remote",
                "remote": false,
                "employmentType": "CONTRACT",
                "experienceLevel": "LEAD",
                "minExperience": 10,
                "maxExperience": 15,
                "salaryMin": 150000,
                "salaryMax": 220000,
                "currency": "USD",
                "department": "Platform",
                "numberOfOpenings": 1,
                "priority": "URGENT",
                "closingDate": "2026-11-30"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobService jobService;

    @MockitoBean
    private ApiKeyService apiKeyService;

    @BeforeEach
    void authenticate() {
        when(apiKeyService.authenticate(API_KEY)).thenReturn(new UsernamePasswordAuthenticationToken(
                new ApiKeyPrincipal(1L, 1L, "org-a-key"), null,
                List.of(new SimpleGrantedAuthority("ROLE_API_KEY"))));
    }

    private JobResponse jobResponse() {
        return JobResponse.builder()
                .id(10L)
                .jobCode("JOB-000010")
                .title("Senior Java Developer")
                .description("Backend development")
                .requirements("Java, Spring Boot")
                .responsibilities("Design APIs")
                .location("Bengaluru")
                .country("India")
                .state("Karnataka")
                .city("Bengaluru")
                .remote(true)
                .employmentType(EmploymentType.FULL_TIME)
                .experienceLevel(ExperienceLevel.SENIOR)
                .minExperience(5)
                .maxExperience(9)
                .salaryMin(new BigDecimal("2000000"))
                .salaryMax(new BigDecimal("3500000"))
                .currency("INR")
                .department("Engineering")
                .numberOfOpenings(2)
                .status(JobStatus.OPEN)
                .priority(JobPriority.HIGH)
                .publishedOn(LocalDateTime.of(2026, 2, 1, 10, 0))
                .closingDate(LocalDate.of(2026, 12, 31))
                .client(JobResponse.ClientRef.builder()
                        .id(5L)
                        .clientCode("CLI-000005")
                        .companyName("Acme Technologies")
                        .build())
                .isActive(true)
                .createdOn(LocalDateTime.of(2026, 1, 1, 10, 0))
                .updatedOn(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();
    }

    private JobSummaryResponse summaryResponse() {
        return JobSummaryResponse.builder()
                .id(10L)
                .jobCode("JOB-000010")
                .title("Senior Java Developer")
                .clientId(5L)
                .clientName("Acme Technologies")
                .location("Bengaluru")
                .remote(true)
                .employmentType(EmploymentType.FULL_TIME)
                .experienceLevel(ExperienceLevel.SENIOR)
                .status(JobStatus.OPEN)
                .priority(JobPriority.HIGH)
                .numberOfOpenings(2)
                .isActive(true)
                .build();
    }

    @Test
    void createReturns201WithStandardEnvelope() throws Exception {
        when(jobService.createJob(eq(1L), any())).thenReturn(jobResponse());

        mockMvc.perform(post("/api/v1/jobs")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Job created successfully"))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.jobCode").value("JOB-000010"))
                .andExpect(jsonPath("$.data.title").value("Senior Java Developer"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.client.id").value(5))
                .andExpect(jsonPath("$.data.client.companyName").value("Acme Technologies"))
                .andExpect(jsonPath("$.metadata").isEmpty())
                .andExpect(jsonPath("$.error").isEmpty())
                .andExpect(jsonPath("$.path").value("/api/v1/jobs"));

        verify(jobService).createJob(eq(1L), any());
    }

    @Test
    void createResponseNeverExposesEntityOrganizationOrCreatedBy() throws Exception {
        when(jobService.createJob(eq(1L), any())).thenReturn(jobResponse());

        mockMvc.perform(post("/api/v1/jobs")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.organization").doesNotExist())
                .andExpect(jsonPath("$.data.createdBy").doesNotExist())
                .andExpect(jsonPath("$.data.isDeleted").doesNotExist());
    }

    @Test
    void createIgnoresClientProvidedJobCodeStatusAndOrganizationId() throws Exception {
        when(jobService.createJob(eq(1L), any())).thenReturn(jobResponse());

        mockMvc.perform(post("/api/v1/jobs")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "jobCode": "JOB-999999",
                                    "organizationId": 99,
                                    "status": "OPEN",
                                    "clientId": 5,
                                    "title": "Senior Java Developer",
                                    "description": "Backend development",
                                    "requirements": "Java, Spring Boot",
                                    "employmentType": "FULL_TIME",
                                    "experienceLevel": "SENIOR",
                                    "numberOfOpenings": 2
                                }
                                """))
                .andExpect(status().isCreated());

        verify(jobService).createJob(eq(1L), any());
    }

    @Test
    void createReturns400ForValidationErrors() throws Exception {
        mockMvc.perform(post("/api/v1/jobs")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "clientId": 5,
                                    "title": "",
                                    "description": "",
                                    "requirements": "",
                                    "employmentType": "FULL_TIME",
                                    "experienceLevel": "SENIOR",
                                    "numberOfOpenings": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.title").value("Title is required"))
                .andExpect(jsonPath("$.error.details.numberOfOpenings").value("Number of openings must be at least 1"));
    }

    @Test
    void listReturnsPaginatedResponseWithMetadata() throws Exception {
        when(jobService.getJobs(eq(1L), any(), any(), any(), any(), any(), any())).thenReturn(
                new PageImpl<>(List.of(jobResponse()), PageRequest.of(0, 20), 25));

        mockMvc.perform(get("/api/v1/jobs").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].jobCode").value("JOB-000010"))
                .andExpect(jsonPath("$.metadata.totalElements").value(25))
                .andExpect(jsonPath("$.metadata.totalPages").value(2))
                .andExpect(jsonPath("$.metadata.page").value(0))
                .andExpect(jsonPath("$.metadata.size").value(20));

        verify(jobService).getJobs(eq(1L), any(), any(), any(), any(), any(), any());
    }

    @Test
    void searchReturnsPaginatedSummaryResponse() throws Exception {
        when(jobService.searchJobs(eq(1L), any(), any(), any(), any(), any())).thenReturn(
                new PageImpl<>(List.of(summaryResponse()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/jobs/search")
                        .header("X-API-KEY", API_KEY)
                        .param("keyword", "java")
                        .param("clientId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].clientName").value("Acme Technologies"))
                .andExpect(jsonPath("$.data[0].description").doesNotExist());

        verify(jobService).searchJobs(eq(1L), eq("java"), eq(5L), any(), any(), any());
    }

    @Test
    void listByClientReturnsPaginatedResponse() throws Exception {
        when(jobService.getJobsByClient(eq(1L), eq(5L), any())).thenReturn(
                new PageImpl<>(List.of(jobResponse()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/clients/5/jobs").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.path").value("/api/v1/clients/5/jobs"));

        verify(jobService).getJobsByClient(eq(1L), eq(5L), any());
    }

    @Test
    void getReturns200ForOwnJob() throws Exception {
        when(jobService.getJobById(1L, 10L)).thenReturn(jobResponse());

        mockMvc.perform(get("/api/v1/jobs/10").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.client.companyName").value("Acme Technologies"));
    }

    @Test
    void getReturns404ForMissingJob() throws Exception {
        when(jobService.getJobById(1L, 99L)).thenThrow(new JobNotFoundException(99L));

        mockMvc.perform(get("/api/v1/jobs/99").header("X-API-KEY", API_KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("JOB_NOT_FOUND"));
    }

    @Test
    void getReturns403ForJobOfAnotherOrganization() throws Exception {
        when(jobService.getJobById(1L, 2L)).thenThrow(new JobAccessDeniedException());

        mockMvc.perform(get("/api/v1/jobs/2").header("X-API-KEY", API_KEY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("JOB_ACCESS_DENIED"));
    }

    @Test
    void updateAppliesProfileChanges() throws Exception {
        when(jobService.updateJob(eq(1L), eq(10L), any())).thenReturn(jobResponse());

        mockMvc.perform(put("/api/v1/jobs/10")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10));

        verify(jobService).updateJob(eq(1L), eq(10L), any());
    }

    @Test
    void changeStatusDelegatesToService() throws Exception {
        when(jobService.updateJobStatus(eq(1L), eq(10L), any())).thenReturn(jobResponse());

        mockMvc.perform(patch("/api/v1/jobs/10/status")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        verify(jobService).updateJobStatus(eq(1L), eq(10L), any());
    }

    @Test
    void changeStatusRequiresAStatus() throws Exception {
        mockMvc.perform(patch("/api/v1/jobs/10/status")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.status").value("Status is required"));
    }

    @Test
    void publishDelegatesToService() throws Exception {
        when(jobService.publishJob(1L, 10L)).thenReturn(jobResponse());

        mockMvc.perform(patch("/api/v1/jobs/10/publish").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Job published successfully"));

        verify(jobService).publishJob(1L, 10L);
    }

    @Test
    void holdDelegatesToService() throws Exception {
        when(jobService.putJobOnHold(1L, 10L)).thenReturn(jobResponse());

        mockMvc.perform(patch("/api/v1/jobs/10/hold").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk());

        verify(jobService).putJobOnHold(1L, 10L);
    }

    @Test
    void closeDelegatesToService() throws Exception {
        when(jobService.closeJob(1L, 10L)).thenReturn(jobResponse());

        mockMvc.perform(patch("/api/v1/jobs/10/close").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk());

        verify(jobService).closeJob(1L, 10L);
    }

    @Test
    void cancelDelegatesToService() throws Exception {
        when(jobService.cancelJob(1L, 10L)).thenReturn(jobResponse());

        mockMvc.perform(patch("/api/v1/jobs/10/cancel").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk());

        verify(jobService).cancelJob(1L, 10L);
    }

    @Test
    void changeClientDelegatesToService() throws Exception {
        when(jobService.changeJobClient(eq(1L), eq(10L), any())).thenReturn(jobResponse());

        mockMvc.perform(patch("/api/v1/jobs/10/client")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":6}"))
                .andExpect(status().isOk());

        verify(jobService).changeJobClient(eq(1L), eq(10L), any());
    }

    @Test
    void changeClientRequiresAClient() throws Exception {
        mockMvc.perform(patch("/api/v1/jobs/10/client")
                        .header("X-API-KEY", API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.clientId").value("Client is required"));
    }

    @Test
    void activateDelegatesToService() throws Exception {
        when(jobService.activateJob(1L, 10L)).thenReturn(jobResponse());

        mockMvc.perform(patch("/api/v1/jobs/10/activate").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk());

        verify(jobService).activateJob(1L, 10L);
    }

    @Test
    void deactivateDelegatesToService() throws Exception {
        when(jobService.deactivateJob(1L, 10L)).thenReturn(jobResponse());

        mockMvc.perform(patch("/api/v1/jobs/10/deactivate").header("X-API-KEY", API_KEY))
                .andExpect(status().isOk());

        verify(jobService).deactivateJob(1L, 10L);
    }

    @Test
    void deleteReturns204NoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/jobs/10").header("X-API-KEY", API_KEY))
                .andExpect(status().isNoContent());

        verify(jobService).deleteJob(1L, 10L);
    }

    @Test
    void deleteReturns403ForAnotherOrganization() throws Exception {
        doThrow(new JobAccessDeniedException()).when(jobService).deleteJob(1L, 2L);

        mockMvc.perform(delete("/api/v1/jobs/2").header("X-API-KEY", API_KEY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("JOB_ACCESS_DENIED"));
    }
}
