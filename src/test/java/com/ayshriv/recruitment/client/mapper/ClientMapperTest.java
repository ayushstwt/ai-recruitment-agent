package com.ayshriv.recruitment.client.mapper;

import com.ayshriv.recruitment.client.dto.request.CreateClientRequest;
import com.ayshriv.recruitment.client.dto.request.UpdateClientRequest;
import com.ayshriv.recruitment.client.dto.response.ClientResponse;
import com.ayshriv.recruitment.client.dto.response.ClientSummaryResponse;
import com.ayshriv.recruitment.client.entity.Client;
import com.ayshriv.recruitment.client.entity.CompanySize;
import com.ayshriv.recruitment.organization.entity.Organization;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ClientMapperTest {

    private final ClientMapper mapper = new ClientMapper();

    private Client client() {
        Client client = new Client();
        client.setId(10L);
        client.setClientCode("CLI-000010");
        client.setCompanyName("Acme Technologies");
        client.setLegalName("Acme Technologies Pvt Ltd");
        client.setEmail("hr@acme.com");
        client.setPhone("+911234567890");
        client.setWebsite("https://acme.com");
        client.setIndustry("Technology");
        client.setCompanySize(CompanySize.MEDIUM);
        client.setCountry("India");
        client.setState("Uttar Pradesh");
        client.setCity("Kanpur");
        client.setAddress("Business Park");
        client.setPostalCode("208001");
        client.setTimezone("Asia/Kolkata");
        client.setDescription("Technology product company");
        client.setNotes("Priority client");
        client.setOrganization(new Organization(99L));
        client.setActive(true);
        client.setDeleted(false);
        client.setCreatedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
        client.setUpdatedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
        return client;
    }

    private CreateClientRequest createRequest() {
        return new CreateClientRequest(
                "Acme Technologies", "Acme Technologies Pvt Ltd", "hr@acme.com", "+911234567890",
                "https://acme.com", "Technology", CompanySize.MEDIUM, "India", "Uttar Pradesh",
                "Kanpur", "Business Park", "208001", "Asia/Kolkata",
                "Technology product company", "Priority client");
    }

    @Test
    void toResponseMapsAllBusinessFields() {
        ClientResponse response = mapper.toResponse(client());

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getClientCode()).isEqualTo("CLI-000010");
        assertThat(response.getCompanyName()).isEqualTo("Acme Technologies");
        assertThat(response.getLegalName()).isEqualTo("Acme Technologies Pvt Ltd");
        assertThat(response.getEmail()).isEqualTo("hr@acme.com");
        assertThat(response.getPhone()).isEqualTo("+911234567890");
        assertThat(response.getWebsite()).isEqualTo("https://acme.com");
        assertThat(response.getIndustry()).isEqualTo("Technology");
        assertThat(response.getCompanySize()).isEqualTo(CompanySize.MEDIUM);
        assertThat(response.getCountry()).isEqualTo("India");
        assertThat(response.getState()).isEqualTo("Uttar Pradesh");
        assertThat(response.getCity()).isEqualTo("Kanpur");
        assertThat(response.getAddress()).isEqualTo("Business Park");
        assertThat(response.getPostalCode()).isEqualTo("208001");
        assertThat(response.getTimezone()).isEqualTo("Asia/Kolkata");
        assertThat(response.getDescription()).isEqualTo("Technology product company");
        assertThat(response.getNotes()).isEqualTo("Priority client");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void toResponseNeverExposesEntityOrOrganization() {
        ClientResponse response = mapper.toResponse(client());

        assertThat(response).isNotInstanceOf(Client.class);
        assertThat(hasField(response, "organization")).isFalse();
        assertThat(hasField(response, "contacts")).isFalse();
        assertThat(hasField(response, "isDeleted")).isFalse();
    }

    @Test
    void toSummaryResponseKeepsPayloadLightweight() {
        ClientSummaryResponse summary = mapper.toSummaryResponse(client());

        assertThat(summary.getId()).isEqualTo(10L);
        assertThat(summary.getClientCode()).isEqualTo("CLI-000010");
        assertThat(summary.getCompanyName()).isEqualTo("Acme Technologies");
        assertThat(summary.getIndustry()).isEqualTo("Technology");
        assertThat(summary.isActive()).isTrue();
        assertThat(hasField(summary, "email")).isFalse();
        assertThat(hasField(summary, "address")).isFalse();
        assertThat(hasField(summary, "notes")).isFalse();
    }

    @Test
    void toEntityMapsCreationRequestWithoutCodeAndOrganization() {
        Client entity = mapper.toEntity(createRequest());

        assertThat(entity.getCompanyName()).isEqualTo("Acme Technologies");
        assertThat(entity.getEmail()).isEqualTo("hr@acme.com");
        assertThat(entity.getCompanySize()).isEqualTo(CompanySize.MEDIUM);
        assertThat(entity.getTimezone()).isEqualTo("Asia/Kolkata");
        assertThat(entity.getClientCode()).isNull();
        assertThat(entity.getOrganization()).isNull();
    }

    @Test
    void updateEntityOverwritesOnlyProfileFields() {
        Client client = client();
        UpdateClientRequest request = new UpdateClientRequest(
                "Acme Global", "Acme Global Pvt Ltd", "careers@acme.com", "+910987654321",
                "https://www.acme.com", "Enterprise Software", CompanySize.LARGE, "India", "Maharashtra",
                "Mumbai", "Tower A", "400001", "Asia/Kolkata",
                "Global product company", "Strategic account");

        mapper.updateEntity(client, request);

        assertThat(client.getCompanyName()).isEqualTo("Acme Global");
        assertThat(client.getEmail()).isEqualTo("careers@acme.com");
        assertThat(client.getCompanySize()).isEqualTo(CompanySize.LARGE);
        assertThat(client.getCity()).isEqualTo("Mumbai");
        assertThat(client.getNotes()).isEqualTo("Strategic account");
        assertThat(client.getClientCode()).isEqualTo("CLI-000010");
        assertThat(client.getOrganizationId()).isEqualTo(99L);
        assertThat(client.isActive()).isTrue();
        assertThat(client.isDeleted()).isFalse();
        assertThat(client.getCreatedOn()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0));
    }

    private boolean hasField(Object object, String fieldName) {
        try {
            object.getClass().getDeclaredField(fieldName);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}