package com.ayshriv.recruitment.user.repository;

import com.ayshriv.recruitment.organization.entity.Organization;
import com.ayshriv.recruitment.organization.repository.OrganizationRepository;
import com.ayshriv.recruitment.role.entity.Role;
import com.ayshriv.recruitment.role.repository.RoleRepository;
import com.ayshriv.recruitment.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the user and role HQL / JPQL queries against the real Flyway
 * schema (H2 in PostgreSQL mode) to catch tenant-scoping and soft-delete
 * mistakes that unit tests with mocks cannot.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:userrolerepo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=true"
})
@org.springframework.transaction.annotation.Transactional
class UserRoleRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    private Organization orgA;
    private Organization orgB;

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
    void flywaySeedsTheSixSystemRoles() {
        assertThat(roleRepository.count()).isEqualTo(6);
        assertThat(roleRepository.findSystemRoles().stream().map(Role::getName))
                .contains("AGENCY_ADMIN", "RECRUITMENT_MANAGER", "RECRUITER", "HIRING_MANAGER",
                        "INTERVIEWER", "VIEWER");
    }

    @Test
    void findByEmailAndOrganizationIsScopedToTheOrganization() {
        User userA = user(orgA, "john@example.com", "Org A user");
        userB(orgB, "john@example.com", "Org B user");

        Optional<User> inA = userRepository.findByEmailAndOrganization("JOHN@example.com", orgA.getId());
        Optional<User> inB = userRepository.findByEmailAndOrganization("john@example.com", orgB.getId());

        assertThat(inA).isPresent();
        assertThat(inB).isPresent();
        assertThat(inA.get().getId()).isEqualTo(userA.getId());
        assertThat(inB.get().getId()).isNotEqualTo(userA.getId());
    }

    @Test
    void findByIdAndOrganizationOnlyReturnsUsersOfTheOrganization() {
        User userA = user(orgA, "john@example.com", "Org A user");

        assertThat(userRepository.findByIdAndOrganization(userA.getId(), orgA.getId())).isPresent();
        assertThat(userRepository.findByIdAndOrganization(userA.getId(), orgB.getId())).isEmpty();
    }

    @Test
    void findByIdAndNotDeletedExcludesSoftDeletedUsers() {
        User userA = user(orgA, "john@example.com", "Org A user");
        userA.softDelete();
        userRepository.save(userA);

        assertThat(userRepository.findByIdAndNotDeleted(userA.getId())).isEmpty();
        assertThat(userRepository.findByIdAndOrganization(userA.getId(), orgA.getId())).isEmpty();
    }

    @Test
    void findAllByOrganizationExcludesSoftDeletedUsers() {
        user(orgA, "john@example.com", "Org A user");
        User deleted = user(orgA, "deleted@example.com", "Deleted user");
        deleted.softDelete();
        userRepository.save(deleted);
        user(orgB, "other@example.com", "Org B user");

        Page<User> page = userRepository.findAllByOrganization(orgA.getId(), PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void searchUsersMatchesFirstNameLastNameEmailAndJobTitle() {
        user(orgA, "john.doe@example.com", "Senior Recruiter");
        User jane = user(orgA, "jane@example.com", "Hiring Manager");
        jane.setFirstName("Jane");
        userRepository.save(jane);
        user(orgB, "john@other.com", "Recruiter");

        assertThat(search(orgA.getId(), "john")).extracting(User::getEmail)
                .containsExactly("john.doe@example.com");
        assertThat(search(orgA.getId(), "SENIOR")).extracting(User::getEmail)
                .containsExactly("john.doe@example.com");
        assertThat(search(orgA.getId(), "jane")).extracting(User::getEmail)
                .containsExactly("jane@example.com");
        assertThat(search(orgB.getId(), "john")).extracting(User::getEmail)
                .containsExactly("john@other.com");
    }

    @Test
    void findAccessibleRoleReturnsOwnAndSystemRolesButNotCrossOrganizationRoles() {
        Role systemRecruiter = roleRepository.findSystemRoles().stream()
                .filter(r -> r.getName().equals("RECRUITER")).findFirst().orElseThrow();
        Role customA = role(orgA, "CUSTOM_A", false);
        Role customB = role(orgB, "CUSTOM_B", false);

        assertThat(roleRepository.findAccessibleRole(systemRecruiter.getId(), orgA.getId())).isPresent();
        assertThat(roleRepository.findAccessibleRole(customA.getId(), orgA.getId())).isPresent();
        assertThat(roleRepository.findAccessibleRole(customB.getId(), orgA.getId())).isEmpty();
        assertThat(roleRepository.findAccessibleRole(customB.getId(), orgB.getId())).isPresent();
    }

    @Test
    void findAccessibleRolesReturnsOwnPlusSystemRolesOnly() {
        role(orgA, "CUSTOM_A", false);
        role(orgB, "CUSTOM_B", false);

        List<Role> roles = roleRepository.findAccessibleRoles(orgA.getId());

        assertThat(roles).allSatisfy(r -> assertThat(
                r.isSystemRole() || r.getOrganizationId().equals(orgA.getId())).isTrue());
        assertThat(roles.stream().map(Role::getName))
                .doesNotContain("CUSTOM_B")
                .contains("CUSTOM_A", "AGENCY_ADMIN", "RECRUITER");
    }

    @Test
    void findByRoleIdAndOrganizationReturnsUsersWithTheRole() {
        Role recruiter = roleRepository.findSystemRoles().stream()
                .filter(r -> r.getName().equals("RECRUITER")).findFirst().orElseThrow();

        User userA = user(orgA, "john@example.com", "Senior Recruiter");
        userA.getRoles().add(recruiter);
        userRepository.save(userA);
        User userB = user(orgB, "jane@example.com", "Recruiter");
        userB.getRoles().add(recruiter);
        userRepository.save(userB);

        Page<User> page = userRepository.findByRoleIdAndOrganization(
                recruiter.getId(), orgA.getId(), PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(userA.getId());
    }

    private Page<User> search(Long organizationId, String keyword) {
        return userRepository.searchUsers(organizationId, keyword, PageRequest.of(0, 20));
    }

    private User user(Organization organization, String email, String jobTitle) {
        User user = new User();
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setEmail(email);
        user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuvwxyz1234567890");
        user.setJobTitle(jobTitle);
        user.setOrganization(organization);
        return userRepository.save(user);
    }

    private User userB(Organization organization, String email, String jobTitle) {
        return user(organization, email, jobTitle);
    }

    private Role role(Organization organization, String name, boolean systemRole) {
        Role role = new Role();
        role.setName(name);
        role.setDescription("Test role " + name);
        role.setSystemRole(systemRole);
        if (!systemRole) {
            role.setOrganization(organization);
        }
        return roleRepository.save(role);
    }
}