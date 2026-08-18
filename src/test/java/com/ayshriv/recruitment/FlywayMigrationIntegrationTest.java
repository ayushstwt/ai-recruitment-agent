package com.ayshriv.recruitment;

import com.ayshriv.recruitment.apiKey.repository.ApiKeyRepository;
import com.ayshriv.recruitment.client.repository.ClientRepository;
import com.ayshriv.recruitment.clientContact.repository.ClientContactRepository;
import com.ayshriv.recruitment.organization.repository.OrganizationRepository;
import com.ayshriv.recruitment.role.repository.RoleRepository;
import com.ayshriv.recruitment.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the Flyway migrations apply cleanly and that the Hibernate
 * entity mappings validate against the resulting schema.
 *
 * <p>Runs against an in-memory H2 database in PostgreSQL compatibility mode
 * so the migrations (which use PostgreSQL syntax such as {@code BIGSERIAL})
 * can be exercised without a live PostgreSQL server.</p>
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:recruitment;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=true"
})
class FlywayMigrationIntegrationTest {

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ClientContactRepository clientContactRepository;

    @Test
    void flywayMigrationsApplyAndJpaSchemaValidates() {
        assertThat(apiKeyRepository.count()).isZero();
        assertThat(organizationRepository.count()).isZero();
        assertThat(userRepository.count()).isZero();
        assertThat(roleRepository.count()).isEqualTo(6);
        assertThat(clientRepository.count()).isZero();
        assertThat(clientContactRepository.count()).isZero();
    }
}
