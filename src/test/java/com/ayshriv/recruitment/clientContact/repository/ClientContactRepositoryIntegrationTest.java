package com.ayshriv.recruitment.clientContact.repository;

import com.ayshriv.recruitment.client.entity.Client;
import com.ayshriv.recruitment.client.repository.ClientRepository;
import com.ayshriv.recruitment.clientContact.entity.ClientContact;
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
 * Exercises the client contact HQL / JPQL queries against the real Flyway
 * schema (H2 in PostgreSQL mode) to catch tenant-scoping and soft-delete
 * mistakes that unit tests with mocks cannot.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:contactrepo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=true"
})
@org.springframework.transaction.annotation.Transactional
class ClientContactRepositoryIntegrationTest {

    @Autowired
    private ClientContactRepository contactRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private Organization orgA;
    private Organization orgB;
    private Client clientA;
    private Client clientB;

    @BeforeEach
    void setUp() {
        Organization organizationA = new Organization("Organization A");
        organizationA.setEmail("org.a@example.com");
        orgA = organizationRepository.save(organizationA);

        Organization organizationB = new Organization("Organization B");
        organizationB.setEmail("org.b@example.com");
        orgB = organizationRepository.save(organizationB);

        clientA = client(orgA, "Acme A", "a@acme.com");
        clientB = client(orgB, "Acme B", "b@acme.com");
    }

    @Test
    void findAllByClientIdAndOrganizationIsScopedToClientAndOrganization() {
        ClientContact a = contact(clientA, "sarah@acme.com", "Sarah", "HR Manager");
        contact(clientA, "jane@acme.com", "Jane", "Recruiter");
        contact(clientB, "tom@acme.com", "Tom", "CTO");

        Page<ClientContact> page =
                contactRepository.findAllByClientIdAndOrganization(clientA.getId(), orgA.getId(),
                        PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(ClientContact::getId)
                .containsExactlyInAnyOrder(a.getId(),
                        page.getContent().stream().filter(c -> c.getEmail().equals("jane@acme.com"))
                                .findFirst().orElseThrow().getId());
    }

    @Test
    void existsByIdAndOrganizationIsScopedToTheOrganization() {
        ClientContact contactA = contact(clientA, "sarah@acme.com", "Sarah", "HR Manager");

        assertThat(contactRepository.existsByIdAndOrganization(contactA.getId(), orgA.getId())).isTrue();
        assertThat(contactRepository.existsByIdAndOrganization(contactA.getId(), orgB.getId())).isFalse();
    }

    @Test
    void findByIdAndNotDeletedExcludesSoftDeletedContacts() {
        ClientContact contactA = contact(clientA, "sarah@acme.com", "Sarah", "HR Manager");
        contactA.softDelete();
        contactRepository.save(contactA);

        assertThat(contactRepository.findByIdAndNotDeleted(contactA.getId())).isEmpty();
        assertThat(contactRepository.existsByIdAndOrganization(contactA.getId(), orgA.getId())).isFalse();
    }

    @Test
    void searchContactsMatchesFirstNameLastNameEmailJobTitleAndDepartment() {
        contact(clientA, "sarah.smith@acme.com", "Sarah", "HR Manager");
        ClientContact jane = contact(clientA, "jane@acme.com", "Jane", "Talent Acquisition");
        jane.setLastName("Doe");
        jane.setDepartment("People Operations");
        contactRepository.save(jane);
        contact(clientB, "sarah@acme.com", "Sarah", "HR Manager");

        assertThat(search(clientA.getId(), orgA.getId(), "sarah")).extracting(ClientContact::getEmail)
                .containsExactly("sarah.smith@acme.com");
        assertThat(search(clientA.getId(), orgA.getId(), "OPERATIONS")).extracting(ClientContact::getEmail)
                .containsExactly("jane@acme.com");
        assertThat(search(clientA.getId(), orgA.getId(), "HR MANAGER")).extracting(ClientContact::getEmail)
                .containsExactly("sarah.smith@acme.com");
        assertThat(search(clientA.getId(), orgA.getId(), "doe")).extracting(ClientContact::getEmail)
                .containsExactly("jane@acme.com");
        assertThat(search(clientB.getId(), orgB.getId(), "sarah")).extracting(ClientContact::getEmail)
                .containsExactly("sarah@acme.com");
    }

    @Test
    void searchContactsNeverLeaksContactsOfOtherOrganizations() {
        contact(clientA, "sarah@acme.com", "Sarah", "HR Manager");
        contact(clientB, "tom@acme.com", "Tom", "CTO");

        Page<ClientContact> page =
                contactRepository.searchContacts(clientA.getId(), orgA.getId(), "tom", PageRequest.of(0, 20));

        assertThat(page.getContent()).isEmpty();
    }

    private Page<ClientContact> search(Long clientId, Long organizationId, String keyword) {
        return contactRepository.searchContacts(clientId, organizationId, keyword, PageRequest.of(0, 20));
    }

    private Client client(Organization organization, String companyName, String email) {
        Client client = new Client();
        client.setClientCode("CLI-000001");
        client.setCompanyName(companyName);
        client.setEmail(email);
        client.setOrganization(organization);
        return clientRepository.save(client);
    }

    private ClientContact contact(Client client, String email, String firstName, String jobTitle) {
        ClientContact contact = new ClientContact();
        contact.setFirstName(firstName);
        contact.setLastName("Smith");
        contact.setEmail(email);
        contact.setJobTitle(jobTitle);
        contact.setClient(client);
        return contactRepository.save(contact);
    }
}