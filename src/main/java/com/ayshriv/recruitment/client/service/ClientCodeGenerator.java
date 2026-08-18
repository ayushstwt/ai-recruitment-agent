package com.ayshriv.recruitment.client.service;

import com.ayshriv.recruitment.client.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Generates the human readable client code ({@code CLI-000001}, ...) for a
 * new client.
 *
 * <p>Codes are allocated sequentially per organization and are never reused.
 * Because the previous code number equals the total number of client rows
 * ever created in the organization (rows are only ever soft deleted), the next
 * code is derived from that count.</p>
 *
 * <p>Concurrency is handled by the caller: {@link ClientService} serializes
 * code allocation per tenant with a pessimistic lock on the organization row,
 * and the {@code (ORGANIZATION_ID, CLIENT_CODE)} unique constraint in the
 * database is the final safety net, so two concurrent creations can never
 * receive the same code.</p>
 */
@Component
@RequiredArgsConstructor
public class ClientCodeGenerator {

    /**
     * Number of digits in the sequential part of a client code.
     */
    private static final int CODE_WIDTH = 6;

    /**
     * Format template for a client code, for example {@code CLI-000001}.
     */
    private static final String CODE_FORMAT = "CLI-%0" + CODE_WIDTH + "d";

    private final ClientRepository clientRepository;

    /**
     * Compute the next client code for an organization.
     *
     * @param organizationId owning tenant
     * @return next sequential client code, for example {@code CLI-000007}
     */
    public String nextCode(Long organizationId) {
        long nextNumber = clientRepository.countByOrganization(organizationId) + 1;
        return String.format(Locale.ROOT, CODE_FORMAT, nextNumber);
    }
}