package com.ayshriv.recruitment.job.controller;

import com.ayshriv.recruitment.common.response.ApiResponse;
import com.ayshriv.recruitment.common.response.ResponseUtil;
import com.ayshriv.recruitment.common.security.SecurityContextService;
import com.ayshriv.recruitment.job.dto.request.ChangeJobClientRequest;
import com.ayshriv.recruitment.job.dto.request.CreateJobRequest;
import com.ayshriv.recruitment.job.dto.request.UpdateJobRequest;
import com.ayshriv.recruitment.job.dto.request.UpdateJobStatusRequest;
import com.ayshriv.recruitment.job.dto.response.JobResponse;
import com.ayshriv.recruitment.job.dto.response.JobSummaryResponse;
import com.ayshriv.recruitment.job.entity.EmploymentType;
import com.ayshriv.recruitment.job.entity.ExperienceLevel;
import com.ayshriv.recruitment.job.entity.JobPriority;
import com.ayshriv.recruitment.job.entity.JobStatus;
import com.ayshriv.recruitment.job.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for job / opening management.
 *
 * <p>Every operation is scoped to the authenticated tenant. The organization
 * is resolved from the API key security context and never accepted from the
 * client; the service rejects any request that targets another organization's
 * job with {@code 403}.</p>
 *
 * <p>The jobs of a client are exposed through the nested
 * {@code /api/v1/clients/{clientId}/jobs} endpoint in
 * {@link JobClientController} so that downstream features can list every
 * opening of a client in one call.</p>
 */
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@SecurityRequirement(name = "X-API-KEY")
@Tag(name = "Jobs", description = "Job / opening management")
public class JobController {

    private final JobService jobService;
    private final SecurityContextService securityContextService;

    /**
     * Create a job within the authenticated organization.
     *
     * @param request     creation payload
     * @param httpRequest servlet request for the response path
     * @return created job
     */
    @Operation(
            summary = "Create job",
            description = "Creates a job for a client of the authenticated organization. "
                    + "The organization is derived from the API key, the job code is generated "
                    + "by the backend and the job always starts in DRAFT; none of these may be "
                    + "supplied in the payload."
    )
    @PostMapping
    public ResponseEntity<ApiResponse<JobResponse>> create(
            @Valid @RequestBody CreateJobRequest request, HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseUtil.success(
                        "Job created successfully",
                        jobService.createJob(currentOrganizationId, request),
                        httpRequest.getRequestURI()));
    }

    /**
     * Page through all non deleted jobs of the authenticated organization.
     *
     * @param status         optional status filter
     * @param priority       optional priority filter
     * @param clientId       optional owning client filter
     * @param employmentType optional employment type filter
     * @param experienceLevel optional experience level filter
     * @param pageable       pagination and sorting
     * @param httpRequest    servlet request for the response path
     * @return paginated list of jobs
     */
    @Operation(
            summary = "List jobs",
            description = "Returns a paginated list of non deleted jobs of the authenticated "
                    + "organization, optionally filtered by status, priority, client, employment "
                    + "type and experience level."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<JobResponse>>> list(
            @RequestParam(required = false)
            @Parameter(description = "Filter by job status")
            JobStatus status,
            @RequestParam(required = false)
            @Parameter(description = "Filter by job priority")
            JobPriority priority,
            @RequestParam(required = false)
            @Parameter(description = "Filter by owning client id")
            Long clientId,
            @RequestParam(required = false)
            @Parameter(description = "Filter by employment type")
            EmploymentType employmentType,
            @RequestParam(required = false)
            @Parameter(description = "Filter by experience level")
            ExperienceLevel experienceLevel,
            @ParameterObject @PageableDefault(size = 20, sort = "createdOn", direction = Sort.Direction.DESC)
            Pageable pageable,
            HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        Page<JobResponse> page = jobService.getJobs(
                currentOrganizationId, status, priority, clientId, employmentType, experienceLevel, pageable);
        return ResponseEntity.ok(ResponseUtil.successPage(
                "Jobs retrieved successfully", page, httpRequest.getRequestURI()));
    }

    /**
     * Search jobs by keyword and filters.
     *
     * @param keyword    optional search keyword across job code, title, description,
     *                   requirements, location and department
     * @param clientId   optional owning client filter
     * @param status     optional status filter
     * @param priority   optional priority filter
     * @param pageable   pagination and sorting
     * @param httpRequest servlet request for the response path
     * @return paginated list of matching jobs
     */
    @Operation(
            summary = "Search jobs",
            description = "Search non deleted jobs of the authenticated organization by keyword "
                    + "across job code, title, description, requirements, location and department, "
                    + "optionally narrowed by client, status and priority."
    )
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<JobSummaryResponse>>> search(
            @RequestParam(required = false)
            @Parameter(description = "Keyword matched against job code, title, description, requirements, location and department")
            String keyword,
            @RequestParam(required = false)
            @Parameter(description = "Filter by owning client id")
            Long clientId,
            @RequestParam(required = false)
            @Parameter(description = "Filter by job status")
            JobStatus status,
            @RequestParam(required = false)
            @Parameter(description = "Filter by job priority")
            JobPriority priority,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdOn", direction = Sort.Direction.DESC)
            Pageable pageable,
            HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        Page<JobSummaryResponse> page = jobService.searchJobs(
                currentOrganizationId, keyword, clientId, status, priority, pageable);
        return ResponseEntity.ok(ResponseUtil.successPage(
                "Jobs retrieved successfully", page, httpRequest.getRequestURI()));
    }

    /**
     * Get a single job by id.
     *
     * @param id          job primary key
     * @param httpRequest servlet request for the response path
     * @return requested job
     */
    @Operation(
            summary = "Get job",
            description = "Returns a single non deleted job. Jobs of another organization "
                    + "are rejected with 403."
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> get(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "Job retrieved successfully",
                jobService.getJobById(currentOrganizationId, id),
                httpRequest.getRequestURI()));
    }

    /**
     * Update a job profile.
     *
     * @param id          job primary key
     * @param request     update payload
     * @param httpRequest servlet request for the response path
     * @return updated job
     */
    @Operation(
            summary = "Update job",
            description = "Updates profile fields of a job. The job code, the tenant, the "
                    + "owning client, the status, lifecycle timestamps and the activation state "
                    + "cannot be modified through this endpoint; the client is changed through "
                    + "the dedicated endpoint and the status through the status endpoints."
    )
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateJobRequest request,
            HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "Job updated successfully",
                jobService.updateJob(currentOrganizationId, id, request),
                httpRequest.getRequestURI()));
    }

    /**
     * Change the status of a job.
     *
     * @param id          job primary key
     * @param request     target status payload
     * @param httpRequest servlet request for the response path
     * @return updated job
     */
    @Operation(
            summary = "Change job status",
            description = "Moves a job to the requested status. The transition must be "
                    + "allowed by the job lifecycle, otherwise the request is rejected with 400."
    )
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<JobResponse>> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateJobStatusRequest request,
            HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "Job status updated successfully",
                jobService.updateJobStatus(currentOrganizationId, id, request),
                httpRequest.getRequestURI()));
    }

    /**
     * Publish a draft job.
     *
     * @param id          job primary key
     * @param httpRequest servlet request for the response path
     * @return published job
     */
    @Operation(
            summary = "Publish job",
            description = "Publishes a draft job: the status becomes OPEN and the published "
                    + "timestamp is set. Publishing a non draft job is rejected with 400."
    )
    @PatchMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<JobResponse>> publish(
            @PathVariable Long id, HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "Job published successfully",
                jobService.publishJob(currentOrganizationId, id),
                httpRequest.getRequestURI()));
    }

    /**
     * Put an open job on hold.
     *
     * @param id          job primary key
     * @param httpRequest servlet request for the response path
     * @return job on hold
     */
    @Operation(
            summary = "Hold job",
            description = "Puts an open job on hold: the status becomes ON_HOLD. Holding a "
                    + "non open job is rejected with 400."
    )
    @PatchMapping("/{id}/hold")
    public ResponseEntity<ApiResponse<JobResponse>> hold(
            @PathVariable Long id, HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "Job put on hold successfully",
                jobService.putJobOnHold(currentOrganizationId, id),
                httpRequest.getRequestURI()));
    }

    /**
     * Close an open or on hold job.
     *
     * @param id          job primary key
     * @param httpRequest servlet request for the response path
     * @return closed job
     */
    @Operation(
            summary = "Close job",
            description = "Closes an open or on hold job: the status becomes CLOSED and the "
                    + "job is deactivated. Closing a job in another status is rejected with 400."
    )
    @PatchMapping("/{id}/close")
    public ResponseEntity<ApiResponse<JobResponse>> close(
            @PathVariable Long id, HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "Job closed successfully",
                jobService.closeJob(currentOrganizationId, id),
                httpRequest.getRequestURI()));
    }

    /**
     * Cancel a draft, open or on hold job.
     *
     * @param id          job primary key
     * @param httpRequest servlet request for the response path
     * @return cancelled job
     */
    @Operation(
            summary = "Cancel job",
            description = "Cancels a draft, open or on hold job: the status becomes CANCELLED "
                    + "and the job is deactivated. Cancelling a job in another status is rejected "
                    + "with 400."
    )
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<JobResponse>> cancel(
            @PathVariable Long id, HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "Job cancelled successfully",
                jobService.cancelJob(currentOrganizationId, id),
                httpRequest.getRequestURI()));
    }

    /**
     * Reassign a job to another client.
     *
     * @param id          job primary key
     * @param request     change client payload
     * @param httpRequest servlet request for the response path
     * @return updated job
     */
    @Operation(
            summary = "Change job client",
            description = "Reassigns a job to another client of the same organization. A "
                    + "client of another organization is rejected with 403."
    )
    @PatchMapping("/{id}/client")
    public ResponseEntity<ApiResponse<JobResponse>> changeClient(
            @PathVariable Long id,
            @Valid @RequestBody ChangeJobClientRequest request,
            HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "Job client changed successfully",
                jobService.changeJobClient(currentOrganizationId, id, request),
                httpRequest.getRequestURI()));
    }

    /**
     * Activate a job.
     *
     * @param id          job primary key
     * @param httpRequest servlet request for the response path
     * @return activated job
     */
    @Operation(
            summary = "Activate job",
            description = "Sets the active flag of a job to true without changing the deleted "
                    + "flag. Closed and cancelled jobs cannot be activated."
    )
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<JobResponse>> activate(
            @PathVariable Long id, HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "Job activated successfully",
                jobService.activateJob(currentOrganizationId, id),
                httpRequest.getRequestURI()));
    }

    /**
     * Deactivate a job.
     *
     * @param id          job primary key
     * @param httpRequest servlet request for the response path
     * @return deactivated job
     */
    @Operation(
            summary = "Deactivate job",
            description = "Sets the active flag of a job to false without deleting the job."
    )
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<JobResponse>> deactivate(
            @PathVariable Long id, HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        return ResponseEntity.ok(ResponseUtil.success(
                "Job deactivated successfully",
                jobService.deactivateJob(currentOrganizationId, id),
                httpRequest.getRequestURI()));
    }

    /**
     * Soft delete a job.
     *
     * @param id job primary key
     * @return empty response
     */
    @Operation(
            summary = "Delete job",
            description = "Soft deletes a job: the active flag is set to false and the "
                    + "deleted flag to true. The record is not physically removed."
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        jobService.deleteJob(currentOrganizationId, id);
        return ResponseEntity.noContent().build();
    }
}