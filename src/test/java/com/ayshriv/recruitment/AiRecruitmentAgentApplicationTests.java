package com.ayshriv.recruitment;

import com.ayshriv.recruitment.apiKey.repository.ApiKeyRepository;
import com.ayshriv.recruitment.client.repository.ClientRepository;
import com.ayshriv.recruitment.clientContact.repository.ClientContactRepository;
import com.ayshriv.recruitment.organization.repository.OrganizationRepository;
import com.ayshriv.recruitment.role.repository.RoleRepository;
import com.ayshriv.recruitment.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,"
                + "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration"
})
class AiRecruitmentAgentApplicationTests {

    @MockitoBean
    private ApiKeyRepository apiKeyRepository;

    @MockitoBean
    private OrganizationRepository organizationRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private ClientRepository clientRepository;

    @MockitoBean
    private ClientContactRepository clientContactRepository;

    @Test
    void contextLoads() {
    }

}