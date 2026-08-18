package com.ayshriv.recruitment.job.service;

import com.ayshriv.recruitment.job.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Generates the human readable job code ({@code JOB-000001}, ...) for a new
 * job.
 *
 * <p>Codes are allocated sequentially per organization and are never reused.
 * Because the previous code number equals the total number of job rows ever
 * created in the organization (rows are only ever soft deleted), the next code
 * is derived from that count.</p>
 *
 * <p>Concurrency is handled by the caller: {@link JobService} serializes code
 * allocation per tenant with a pessimistic lock on the organization row, and
 * the {@code (ORGANIZATION_ID, JOB_CODE)} unique constraint in the database is
 * the final safety net, so two concurrent creations can never receive the same
 * code.</p>
 */
@Component
@RequiredArgsConstructor
public class JobCodeGenerator {

    /**
     * Number of digits in the sequential part of a job code.
     */
    private static final int CODE_WIDTH = 6;

    /**
     * Format template for a job code, for example {@code JOB-000001}.
     */
    private static final String CODE_FORMAT = "JOB-%0" + CODE_WIDTH + "d";

    private final JobRepository jobRepository;

    /**
     * Compute the next job code for an organization.
     *
     * @param organizationId owning tenant
     * @return next sequential job code, for example {@code JOB-000007}
     */
    public String nextCode(Long organizationId) {
        long nextNumber = jobRepository.countByOrganization(organizationId) + 1;
        return String.format(Locale.ROOT, CODE_FORMAT, nextNumber);
    }
}
