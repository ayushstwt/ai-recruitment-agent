package com.ayshriv.recruitment.clientContact.mapper;

import com.ayshriv.recruitment.clientContact.dto.request.CreateClientContactRequest;
import com.ayshriv.recruitment.clientContact.dto.request.UpdateClientContactRequest;
import com.ayshriv.recruitment.clientContact.dto.response.ClientContactResponse;
import com.ayshriv.recruitment.clientContact.entity.ClientContact;
import org.springframework.stereotype.Component;

/**
 * Converts between client contact request / response DTOs and the JPA entity.
 *
 * <p>Response mapping never exposes the owning client entity or the owning
 * organization entity. The client association is set by the service after the
 * tenant ownership check.</p>
 */
@Component
public class ClientContactMapper {

    /**
     * Map an entity into its response representation.
     *
     * @param contact source entity
     * @return response DTO
     */
    public ClientContactResponse toResponse(ClientContact contact) {
        return ClientContactResponse.builder()
                .id(contact.getId())
                .firstName(contact.getFirstName())
                .lastName(contact.getLastName())
                .email(contact.getEmail())
                .phone(contact.getPhone())
                .jobTitle(contact.getJobTitle())
                .department(contact.getDepartment())
                .notes(contact.getNotes())
                .isActive(contact.isActive())
                .createdOn(contact.getCreatedOn())
                .updatedOn(contact.getUpdatedOn())
                .build();
    }

    /**
     * Build a transient entity from a creation request. Only business profile
     * fields are mapped; the client association is set by the service.
     *
     * @param request creation payload
     * @return transient entity
     */
    public ClientContact toEntity(CreateClientContactRequest request) {
        ClientContact contact = new ClientContact();
        applyProfileFields(contact, request.getFirstName(), request.getLastName(), request.getEmail(),
                request.getPhone(), request.getJobTitle(), request.getDepartment(), request.getNotes());
        return contact;
    }

    /**
     * Apply updatable profile fields from an update request onto an existing
     * entity. Immutable fields (id, client, timestamps, active, deleted) are
     * never touched.
     *
     * @param contact target entity
     * @param request update payload
     */
    public void updateEntity(ClientContact contact, UpdateClientContactRequest request) {
        applyProfileFields(contact, request.getFirstName(), request.getLastName(), request.getEmail(),
                request.getPhone(), request.getJobTitle(), request.getDepartment(), request.getNotes());
    }

    /**
     * Copy the shared profile fields onto an entity.
     */
    private void applyProfileFields(ClientContact contact, String firstName, String lastName, String email,
                                    String phone, String jobTitle, String department, String notes) {
        contact.setFirstName(firstName);
        contact.setLastName(lastName);
        contact.setEmail(email);
        contact.setPhone(phone);
        contact.setJobTitle(jobTitle);
        contact.setDepartment(department);
        contact.setNotes(notes);
    }
}