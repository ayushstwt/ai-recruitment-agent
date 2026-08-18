package com.ayshriv.recruitment.clientContact.mapper;

import com.ayshriv.recruitment.clientContact.dto.request.CreateClientContactRequest;
import com.ayshriv.recruitment.clientContact.dto.request.UpdateClientContactRequest;
import com.ayshriv.recruitment.clientContact.dto.response.ClientContactResponse;
import com.ayshriv.recruitment.clientContact.entity.ClientContact;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ClientContactMapperTest {

    private final ClientContactMapper mapper = new ClientContactMapper();

    private ClientContact contact() {
        ClientContact contact = new ClientContact();
        contact.setId(10L);
        contact.setFirstName("Sarah");
        contact.setLastName("Smith");
        contact.setEmail("sarah@acme.com");
        contact.setPhone("+911234567890");
        contact.setJobTitle("HR Manager");
        contact.setDepartment("Human Resources");
        contact.setNotes("Primary hiring contact");
        contact.setActive(true);
        contact.setDeleted(false);
        contact.setCreatedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
        contact.setUpdatedOn(LocalDateTime.of(2026, 1, 1, 10, 0));
        return contact;
    }

    private CreateClientContactRequest createRequest() {
        return new CreateClientContactRequest(
                "Sarah", "Smith", "sarah@acme.com", "+911234567890",
                "HR Manager", "Human Resources", "Primary hiring contact");
    }

    @Test
    void toResponseMapsAllProfileFields() {
        ClientContactResponse response = mapper.toResponse(contact());

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getFirstName()).isEqualTo("Sarah");
        assertThat(response.getLastName()).isEqualTo("Smith");
        assertThat(response.getEmail()).isEqualTo("sarah@acme.com");
        assertThat(response.getPhone()).isEqualTo("+911234567890");
        assertThat(response.getJobTitle()).isEqualTo("HR Manager");
        assertThat(response.getDepartment()).isEqualTo("Human Resources");
        assertThat(response.getNotes()).isEqualTo("Primary hiring contact");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void toResponseNeverExposesClientOrOrganization() {
        ClientContactResponse response = mapper.toResponse(contact());

        assertThat(response).isNotInstanceOf(ClientContact.class);
        assertThat(hasField(response, "client")).isFalse();
        assertThat(hasField(response, "organization")).isFalse();
        assertThat(hasField(response, "isDeleted")).isFalse();
    }

    @Test
    void toEntityMapsCreationRequestWithoutClient() {
        ClientContact entity = mapper.toEntity(createRequest());

        assertThat(entity.getFirstName()).isEqualTo("Sarah");
        assertThat(entity.getEmail()).isEqualTo("sarah@acme.com");
        assertThat(entity.getDepartment()).isEqualTo("Human Resources");
        assertThat(entity.getClient()).isNull();
    }

    @Test
    void updateEntityOverwritesOnlyProfileFields() {
        ClientContact contact = contact();
        UpdateClientContactRequest request = new UpdateClientContactRequest(
                "Sara", "Smith", "sara@acme.com", "+910987654321",
                "Senior HR Manager", "People Operations", "Senior contact");

        mapper.updateEntity(contact, request);

        assertThat(contact.getFirstName()).isEqualTo("Sara");
        assertThat(contact.getEmail()).isEqualTo("sara@acme.com");
        assertThat(contact.getJobTitle()).isEqualTo("Senior HR Manager");
        assertThat(contact.getDepartment()).isEqualTo("People Operations");
        assertThat(contact.getNotes()).isEqualTo("Senior contact");
        assertThat(contact.getId()).isEqualTo(10L);
        assertThat(contact.isActive()).isTrue();
        assertThat(contact.isDeleted()).isFalse();
        assertThat(contact.getCreatedOn()).isEqualTo(LocalDateTime.of(2026, 1, 1, 10, 0));
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