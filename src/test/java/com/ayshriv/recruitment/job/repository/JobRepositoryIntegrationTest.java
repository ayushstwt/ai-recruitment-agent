package com.ayshriv.recruitment.job.repository;

import com.ayshriv.recruitment.client.entity.Client;
import com.ayshriv.recruitment.client.entity.CompanySize;
import com.ayshriv.recruitment.client.repository.ClientRepository;
import com.ayshriv.recruitment.job.entity.EmploymentType;
import com.ayshriv.recruitment.job.entity.ExperienceLevel;
import com.ayshriv.recruitment.job.entity.Job;
import com.ayshriv.recruitment.job.entity.JobPriority;
import com.ayshriv.recruitment.job.entity.JobStatus;
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
 * Exercises the job HQL / JPQL queries against the real Flyway schema (H2 in
 * PostgreSQL mode) to catch tenant-scoping and soft-delete mistakes that unit
 * tests with mocks cannot.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:jobrepo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=true"
})
@org.springframework.transaction.annotation.Transactional
class JobRepositoryIntegrationTest {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private ClientRepository clientRepository;

    private Organization orgA;
    private Organization orgB;
    private Client clientA;
    private Client clientB;
    private long codeCounter;

    @BeforeEach
    void setUp() {
        Organization organizationA = new Organization("Organization A");
        organizationA.setEmail("org.a@example.com");
        orgA = organizationRepository.save(organizationA);

        Organization organizationB = new Organization("Organization B");
        organizationB.setEmail("org.b@example.com");
        orgB = organizationRepository.save(organizationB);

        clientA = client(orgA, "a@acme.com", "Acme A");
        clientB = client(orgB, "b@acme.com", "Acme B");
    }

    @Test
    void findByIdAndOrganizationOnlyReturnsJobsOfTheOrganization() {
        Job jobA = job(orgA, clientA, "Backend Engineer", JobStatus.OPEN);

        assertThat(jobRepository.findByIdAndOrganization(jobA.getId(), orgA.getId())).isPresent();
        assertThat(jobRepository.findByIdAndOrganization(jobA.getId(), orgB.getId())).isEmpty();
    }

    @Test
    void findByJobCodeAndOrganizationAllowsSameCodeInDifferentOrganizations() {
        Job jobA = job(orgA, clientA, "Backend Engineer", JobStatus.OPEN);
        jobA.setJobCode("JOB-000001");
        jobRepository.save(jobA);
        Job jobB = job(orgB, clientB, "Backend Engineer", JobStatus.OPEN);
        jobB.setJobCode("JOB-000001");
        jobRepository.save(jobB);

        assertThat(jobRepository.findByJobCodeAndOrganization("job-000001", orgA.getId()))
                .map(Job::getId).contains(jobA.getId());
        assertThat(jobRepository.findByJobCodeAndOrganization("JOB-000001", orgB.getId()))
                .map(Job::getId).contains(jobB.getId());
    }

    @Test
    void findByIdAndNotDeletedExcludesSoftDeletedJobs() {
        Job jobA = job(orgA, clientA, "Backend Engineer", JobStatus.OPEN);
        jobA.softDelete();
        jobRepository.save(jobA);

        assertThat(jobRepository.findByIdAndNotDeleted(jobA.getId())).isEmpty();
        assertThat(jobRepository.findByIdAndOrganization(jobA.getId(), orgA.getId())).isEmpty();
    }

    @Test
    void findJobsByOrganizationExcludesSoftDeletedAndOtherOrganizations() {
        job(orgA, clientA, "Backend Engineer", JobStatus.OPEN);
        Job deleted = job(orgA, clientA, "Deleted Role", JobStatus.OPEN);
        deleted.softDelete();
        jobRepository.save(deleted);
        job(orgB, clientB, "Backend Engineer", JobStatus.OPEN);

        Page<Job> page = jobRepository.findJobsByOrganization(orgA.getId(), PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getTitle()).isEqualTo("Backend Engineer");
    }

    @Test
    void findActiveJobsExcludesInactiveAndDeleted() {
        Job active = job(orgA, clientA, "Backend Engineer", JobStatus.OPEN);
        Job inactive = job(orgA, clientA, "Inactive Role", JobStatus.OPEN);
        inactive.setActive(false);
        jobRepository.save(inactive);
        job(orgB, clientB, "Backend Engineer", JobStatus.OPEN);

        Page<Job> page = jobRepository.findActiveJobs(orgA.getId(), PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(active.getId());
    }

    @Test
    void findJobsByClientIsScopedToClientAndOrganization() {
        job(orgA, clientA, "Backend Engineer", JobStatus.OPEN);
        job(orgA, clientA, "Frontend Engineer", JobStatus.OPEN);
        job(orgA, client(orgA, "other@acme.com", "Other Client"), "Data Engineer", JobStatus.OPEN);
        job(orgB, clientB, "Backend Engineer", JobStatus.OPEN);

        Page<Job> page = jobRepository.findJobsByClient(clientA.getId(), orgA.getId(), PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).extracting(Job::getTitle)
                .containsExactlyInAnyOrder("Backend Engineer", "Frontend Engineer");
    }

    @Test
    void searchJobsMatchesAcrossFieldsCaseInsensitively() {
        Job backend = job(orgA, clientA, "Backend Engineer", JobStatus.OPEN);
        backend.setDescription("Develop REST APIs in Java and Spring");
        backend.setRequirements("Java, Spring Boot");
        backend.setLocation("Bengaluru");
        backend.setDepartment("Engineering");
        jobRepository.save(backend);

        Job frontend = job(orgA, clientA, "Frontend Engineer", JobStatus.OPEN);
        frontend.setRequirements("React, TypeScript");
        jobRepository.save(frontend);

        job(orgB, clientB, "Backend Engineer", JobStatus.OPEN);

        assertThat(search(orgA.getId(), "backend")).extracting(Job::getId)
                .containsExactly(backend.getId());
        assertThat(search(orgA.getId(), "REST")).extracting(Job::getId)
                .containsExactly(backend.getId());
        assertThat(search(orgA.getId(), "react")).extracting(Job::getId)
                .containsExactly(frontend.getId());
        assertThat(search(orgA.getId(), "BENGALURU")).extracting(Job::getId)
                .containsExactly(backend.getId());
        assertThat(search(orgA.getId(), "engineer")).hasSize(2);
        assertThat(search(orgB.getId(), "backend")).extracting(Job::getId)
                .hasSize(1);
    }

    @Test
    void findJobsByStatusFiltersWithinOrganization() {
        Job open = job(orgA, clientA, "Open Role", JobStatus.OPEN);
        job(orgA, clientA, "Closed Role", JobStatus.CLOSED);
        job(orgB, clientB, "Open Role B", JobStatus.OPEN);

        Page<Job> page = jobRepository.findJobsByStatus(orgA.getId(), JobStatus.OPEN, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(open.getId());
    }

    @Test
    void findJobsByPriorityFiltersWithinOrganization() {
        Job urgent = job(orgA, clientA, "Urgent Role", JobStatus.OPEN);
        urgent.setPriority(JobPriority.URGENT);
        jobRepository.save(urgent);
        job(orgA, clientA, "Normal Role", JobStatus.OPEN);
        Job urgentB = job(orgB, clientB, "Urgent Role B", JobStatus.OPEN);
        urgentB.setPriority(JobPriority.URGENT);
        jobRepository.save(urgentB);

        Page<Job> page = jobRepository.findJobsByPriority(orgA.getId(), JobPriority.URGENT, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(urgent.getId());
    }

    @Test
    void findJobsByEmploymentTypeFiltersWithinOrganization() {
        Job contract = job(orgA, clientA, "Contract Role", JobStatus.OPEN);
        contract.setEmploymentType(EmploymentType.CONTRACT);
        jobRepository.save(contract);
        job(orgA, clientA, "Full Time Role", JobStatus.OPEN);
        Job contractB = job(orgB, clientB, "Contract Role B", JobStatus.OPEN);
        contractB.setEmploymentType(EmploymentType.CONTRACT);
        jobRepository.save(contractB);

        Page<Job> page = jobRepository.findJobsByEmploymentType(
                orgA.getId(), EmploymentType.CONTRACT, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(contract.getId());
    }

    @Test
    void findJobsByExperienceLevelFiltersWithinOrganization() {
        Job senior = job(orgA, clientA, "Senior Role", JobStatus.OPEN);
        senior.setExperienceLevel(ExperienceLevel.SENIOR);
        jobRepository.save(senior);
        job(orgA, clientA, "Junior Role", JobStatus.OPEN);
        Job seniorB = job(orgB, clientB, "Senior Role B", JobStatus.OPEN);
        seniorB.setExperienceLevel(ExperienceLevel.SENIOR);
        jobRepository.save(seniorB);

        Page<Job> page = jobRepository.findJobsByExperienceLevel(
                orgA.getId(), ExperienceLevel.SENIOR, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(senior.getId());
    }

    @Test
    void countByOrganizationCountsAllRowsPerOrganizationIncludingDeleted() {
        job(orgA, clientA, "Role A", JobStatus.OPEN);
        Job deleted = job(orgA, clientA, "Deleted Role", JobStatus.OPEN);
        deleted.softDelete();
        jobRepository.save(deleted);
        job(orgB, clientB, "Role B", JobStatus.OPEN);

        assertThat(jobRepository.countByOrganization(orgA.getId())).isEqualTo(2);
        assertThat(jobRepository.countByOrganization(orgB.getId())).isEqualTo(1);
    }

    private Page<Job> search(Long organizationId, String keyword) {
        return jobRepository.searchJobs(organizationId, keyword, PageRequest.of(0, 20));
    }

    private Client client(Organization organization, String email, String companyName) {
        Client client = new Client();
        client.setClientCode(String.format("CLI-%06d", ++codeCounter));
        client.setCompanyName(companyName);
        client.setEmail(email);
        client.setCompanySize(CompanySize.MEDIUM);
        client.setOrganization(organization);
        return clientRepository.save(client);
    }

    private Job job(Organization organization, Client client, String title, JobStatus status) {
        Job job = new Job();
        job.setJobCode(String.format("JOB-%06d", ++codeCounter));
        job.setTitle(title);
        job.setDescription("Description for " + title);
        job.setRequirements("General requirements");
        job.setNumberOfOpenings(1);
        job.setStatus(status);
        job.setPriority(JobPriority.MEDIUM);
        job.setEmploymentType(EmploymentType.FULL_TIME);
        job.setExperienceLevel(ExperienceLevel.MID);
        job.setRemote(false);
        job.setOrganization(organization);
        job.setClient(client);
        job.setActive(true);
        return jobRepository.save(job);
    }
}
