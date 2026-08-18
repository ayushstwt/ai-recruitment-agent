package com.ayshriv.recruitment.client.repository;

import com.ayshriv.recruitment.client.entity.Client;
import com.ayshriv.recruitment.client.entity.CompanySize;
import com.ayshriv.recruitment.organization.entity.Organization;
import com.ayshriv.recruitment.organization.repository.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the client HQL / JPQL queries against the real Flyway schema (H2
 * in PostgreSQL mode) to catch tenant-scoping and soft-delete mistakes that
 * unit tests with mocks cannot.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:clientrepo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=true"
})
@org.springframework.transaction.annotation.Transactional
class ClientRepositoryIntegrationTest {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private Organization orgA;
    private Organization orgB;
    private long codeCounter;

    @BeforeEach
    void setUp() {
        Organization organizationA = new Organization("Organization A");
        organizationA.setEmail("org.a@example.com");
        orgA = organizationRepository.save(organizationA);

        Organization organizationB = new Organization("Organization B");
        organizationB.setEmail("org.b@example.com");
        orgB = organizationRepository.save(organizationB);
    }

    @Test
    void findByEmailAndOrganizationIsScopedToTheOrganization() {
        Client clientA = client(orgA, "hr@acme.com", "Acme A", CompanySize.MEDIUM);
        client(orgB, "hr@acme.com", "Acme B", CompanySize.LARGE);

        Optional<Client> inA = clientRepository.findByEmailAndOrganization("HR@acme.com", orgA.getId());
        Optional<Client> inB = clientRepository.findByEmailAndOrganization("hr@acme.com", orgB.getId());

        assertThat(inA).isPresent();
        assertThat(inB).isPresent();
        assertThat(inA.get().getId()).isEqualTo(clientA.getId());
        assertThat(inB.get().getId()).isNotEqualTo(clientA.getId());
    }

    @Test
    void findByClientCodeAndOrganizationAllowsSameCodeInDifferentOrganizations() {
        Client clientA = client(orgA, "a@acme.com", "Acme A", null);
        clientA.setClientCode("CLI-000001");
        clientRepository.save(clientA);
        Client clientB = client(orgB, "b@acme.com", "Acme B", null);
        clientB.setClientCode("CLI-000001");
        clientRepository.save(clientB);

        assertThat(clientRepository.findByClientCodeAndOrganization("cli-000001", orgA.getId()))
                .map(Client::getId).contains(clientA.getId());
        assertThat(clientRepository.findByClientCodeAndOrganization("CLI-000001", orgB.getId()))
                .map(Client::getId).contains(clientB.getId());
    }

    @Test
    void findByIdAndOrganizationOnlyReturnsClientsOfTheOrganization() {
        Client clientA = client(orgA, "a@acme.com", "Acme A", null);

        assertThat(clientRepository.findByIdAndOrganization(clientA.getId(), orgA.getId())).isPresent();
        assertThat(clientRepository.findByIdAndOrganization(clientA.getId(), orgB.getId())).isEmpty();
    }

    @Test
    void existsByIdAndOrganizationIsScopedToTheOrganization() {
        Client clientA = client(orgA, "a@acme.com", "Acme A", null);

        assertThat(clientRepository.existsByIdAndOrganization(clientA.getId(), orgA.getId())).isTrue();
        assertThat(clientRepository.existsByIdAndOrganization(clientA.getId(), orgB.getId())).isFalse();
    }

    @Test
    void findByIdAndNotDeletedExcludesSoftDeletedClients() {
        Client clientA = client(orgA, "a@acme.com", "Acme A", null);
        clientA.softDelete();
        clientRepository.save(clientA);

        assertThat(clientRepository.findByIdAndNotDeleted(clientA.getId())).isEmpty();
        assertThat(clientRepository.findByIdAndOrganization(clientA.getId(), orgA.getId())).isEmpty();
    }

    @Test
    void findAllByOrganizationExcludesSoftDeletedAndOtherOrganizations() {
        client(orgA, "a@acme.com", "Acme A", null);
        Client deleted = client(orgA, "deleted@acme.com", "Deleted", null);
        deleted.softDelete();
        clientRepository.save(deleted);
        client(orgB, "b@acme.com", "Acme B", null);

        Page<Client> page = clientRepository.findAllByOrganization(orgA.getId(), PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getEmail()).isEqualTo("a@acme.com");
    }

    @Test
    void findActiveClientsExcludesInactiveAndDeleted() {
        Client active = client(orgA, "a@acme.com", "Acme A", null);
        Client inactive = client(orgA, "inactive@acme.com", "Inactive", null);
        inactive.setActive(false);
        clientRepository.save(inactive);
        client(orgB, "b@acme.com", "Acme B", null);

        Page<Client> page = clientRepository.findActiveClients(orgA.getId(), PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(active.getId());
    }

    @Test
    void searchClientsMatchesAcrossCompanyNameLegalNameEmailClientCodeAndIndustry() {
        Client john = client(orgA, "john.doe@acme.com", "Acme Technologies", CompanySize.STARTUP);
        john.setIndustry("Software Services");
        clientRepository.save(john);
        Client acmeGlobal = client(orgA, "careers@acmeglobal.com", "Acme Global",
                CompanySize.ENTERPRISE);
        client(orgB, "hr@acme.com", "Acme B", CompanySize.SMALL);

        assertThat(search(orgA.getId(), "techno")).extracting(Client::getEmail)
                .containsExactly("john.doe@acme.com");
        assertThat(search(orgA.getId(), "GLOBAL")).extracting(Client::getEmail)
                .containsExactly("careers@acmeglobal.com");
        assertThat(search(orgA.getId(), "SOFTWARE")).extracting(Client::getEmail)
                .containsExactly("john.doe@acme.com");
        assertThat(search(orgA.getId(), acmeGlobal.getClientCode()))
                .extracting(Client::getEmail)
                .containsExactly("careers@acmeglobal.com");
        assertThat(search(orgB.getId(), "acme")).extracting(Client::getEmail)
                .containsExactly("hr@acme.com");
    }

    @Test
    void countByOrganizationCountsAllRowsPerOrganizationIncludingDeleted() {
        client(orgA, "a@acme.com", "Acme A", null);
        Client deleted = client(orgA, "deleted@acme.com", "Deleted", null);
        deleted.softDelete();
        clientRepository.save(deleted);
        client(orgB, "b@acme.com", "Acme B", null);

        assertThat(clientRepository.countByOrganization(orgA.getId())).isEqualTo(2);
        assertThat(clientRepository.countByOrganization(orgB.getId())).isEqualTo(1);
    }

    private Page<Client> search(Long organizationId, String keyword) {
        return clientRepository.searchClients(organizationId, keyword, PageRequest.of(0, 20));
    }

    private Client client(Organization organization, String email, String companyName,
                          CompanySize companySize) {
        Client client = new Client();
        client.setClientCode(String.format("CLI-%06d", ++codeCounter));
        client.setCompanyName(companyName);
        client.setEmail(email);
        client.setCompanySize(companySize);
        client.setOrganization(organization);
        return clientRepository.save(client);
    }
}