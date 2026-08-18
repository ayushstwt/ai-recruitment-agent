package com.ayshriv.recruitment.client.specification;

import com.ayshriv.recruitment.client.entity.Client;
import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable JPA specifications for {@link Client}.
 *
 * <p>Complements the HQL / JPQL queries in {@code ClientRepository} for the
 * cases where a query is assembled from optional filters at runtime. Every
 * specification is tenant-aware: no client can ever be selected without its
 * owning organization id, keeping the hard isolation boundary intact.</p>
 */
public final class ClientSpecification {

    private ClientSpecification() {
    }

    /**
     * Client belonging to the given organization.
     *
     * @param organizationId owning tenant
     * @return specification
     */
    public static Specification<Client> belongsToOrganization(Long organizationId) {
        return (root, query, builder) -> builder.equal(
                root.get("organization").get("id"), organizationId);
    }

    /**
     * Client that has not been soft deleted.
     *
     * @return specification
     */
    public static Specification<Client> notDeleted() {
        return (root, query, builder) -> builder.equal(root.get("isDeleted"), false);
    }

    /**
     * Client that is currently active.
     *
     * @return specification
     */
    public static Specification<Client> active() {
        return (root, query, builder) -> builder.equal(root.get("isActive"), true);
    }

    /**
     * Client whose company name, legal name, email, client code or industry
     * contains the keyword, ignoring case.
     *
     * @param keyword search keyword, never blank
     * @return specification
     */
    public static Specification<Client> keyword(String keyword) {
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, builder) -> {
            var lower = builder.lower(root.get("companyName"));
            var legalName = builder.lower(root.get("legalName"));
            var email = builder.lower(root.get("email"));
            var clientCode = builder.lower(root.get("clientCode"));
            var industry = builder.lower(root.get("industry"));
            return builder.or(
                    builder.like(lower, pattern),
                    builder.like(legalName, pattern),
                    builder.like(email, pattern),
                    builder.like(clientCode, pattern),
                    builder.like(industry, pattern));
        };
    }
}