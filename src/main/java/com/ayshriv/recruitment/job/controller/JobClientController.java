package com.ayshriv.recruitment.job.controller;

import com.ayshriv.recruitment.common.response.ApiResponse;
import com.ayshriv.recruitment.common.response.ResponseUtil;
import com.ayshriv.recruitment.common.security.SecurityContextService;
import com.ayshriv.recruitment.job.dto.response.JobResponse;
import com.ayshriv.recruitment.job.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoints for the jobs of a client.
 *
 * <p>Exposes every opening of a client through the nested
 * {@code /api/v1/clients/{clientId}/jobs} endpoint. The tenant is resolved
 * from the API key security context and never accepted from the client; a
 * client of another organization is rejected with {@code 403}.</p>
 */
@RestController
@RequestMapping("/api/v1/clients/{clientId}/jobs")
@RequiredArgsConstructor
@SecurityRequirement(name = "X-API-KEY")
@Tag(name = "Jobs", description = "Job / opening management")
public class JobClientController {

    private final JobService jobService;
    private final SecurityContextService securityContextService;

    /**
     * Page through all non deleted jobs of a client within the authenticated
     * organization.
     *
     * @param clientId    owning client id
     * @param pageable    pagination and sorting
     * @param httpRequest servlet request for the response path
     * @return paginated list of jobs of the client
     */
    @Operation(
            summary = "List client jobs",
            description = "Returns a paginated list of non deleted jobs of a client of the "
                    + "authenticated organization."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<List<JobResponse>>> listByClient(
            @PathVariable Long clientId,
            @ParameterObject @PageableDefault(size = 20, sort = "createdOn", direction = Sort.Direction.DESC)
            Pageable pageable,
            HttpServletRequest httpRequest) {
        Long currentOrganizationId = securityContextService.getCurrentOrganizationId();
        Page<JobResponse> page = jobService.getJobsByClient(currentOrganizationId, clientId, pageable);
        return ResponseEntity.ok(ResponseUtil.successPage(
                "Client jobs retrieved successfully", page, httpRequest.getRequestURI()));
    }
}
