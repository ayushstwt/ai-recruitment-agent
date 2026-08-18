package com.ayshriv.recruitment.clientContact.service;

import com.ayshriv.recruitment.client.service.ClientService;
import com.ayshriv.recruitment.clientContact.dto.request.CreateClientContactRequest;
import com.ayshriv.recruitment.clientContact.dto.request.UpdateClientContactRequest;
import com.ayshriv.recruitment.clientContact.dto.response.ClientContactResponse;
import com.ayshriv.recruitment.clientContact.entity.ClientContact;
import com.ayshriv.recruitment.clientContact.exception.ClientContactAccessDeniedException;
import com.ayshriv.recruitment.clientContact.exception.ClientContactNotFoundException;
import com.ayshriv.recruitment.clientContact.mapper.ClientContactMapper;
import com.ayshriv.recruitment.clientContact.repository.ClientContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for client contact management.
 *
 * <p>Contacts are nested under a client ({@code /clients/{clientId}/contacts}),
 * so every operation verifies both that the owning client belongs to the
 * authenticated organization and that the contact belongs to the addressed
 * client. A contact of another organization or another client surfaces as
 * {@code 403 FORBIDDEN}, a missing or soft-deleted contact as {@code 404 NOT
 * FOUND}.</p>
 *
 * <p>Deletion is always a soft delete: the record is never physically
 * removed.</p>
 */
@Service
@RequiredArgsConstructor
public class ClientContactService {

    private final ClientContactRepository contactRepository;
    private final ClientContactMapper contactMapper;
    private final ClientService clientService;

    /**
     * Create a contact for a client of the authenticated organization.
     *
     * @param organizationId authenticated tenant id
     * @param clientId       owning client id
     * @param request        creation payload
     * @return created contact
     */
    @Transactional
    public ClientContactResponse createContact(Long organizationId, Long clientId,
                                               CreateClientContactRequest request) {
        ClientContact contact = contactMapper.toEntity(request);
        contact.setClient(clientService.getClientEntity(organizationId, clientId));
        return contactMapper.toResponse(contactRepository.save(contact));
    }

    /**
     * Get a single contact of a client.
     *
     * @param organizationId authenticated tenant id
     * @param clientId       owning client id
     * @param contactId      requested contact id
     * @return requested contact
     */
    @Transactional(readOnly = true)
    public ClientContactResponse getContact(Long organizationId, Long clientId, Long contactId) {
        return contactMapper.toResponse(findContactInOrganization(organizationId, clientId, contactId));
    }

    /**
     * Page through all non deleted contacts of a client.
     *
     * @param organizationId authenticated tenant id
     * @param clientId       owning client id
     * @param pageable       pagination and sorting
     * @return page of contacts
     */
    @Transactional(readOnly = true)
    public Page<ClientContactResponse> getContacts(Long organizationId, Long clientId, Pageable pageable) {
        clientService.getClientEntity(organizationId, clientId);
        return contactRepository.findAllByClientIdAndOrganization(clientId, organizationId, pageable)
                .map(contactMapper::toResponse);
    }

    /**
     * Search non deleted contacts of a client by keyword.
     *
     * @param organizationId authenticated tenant id
     * @param clientId       owning client id
     * @param keyword        search keyword; blank returns all contacts
     * @param pageable       pagination and sorting
     * @return page of matching contacts
     */
    @Transactional(readOnly = true)
    public Page<ClientContactResponse> searchContacts(Long organizationId, Long clientId, String keyword,
                                                      Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return getContacts(organizationId, clientId, pageable);
        }
        clientService.getClientEntity(organizationId, clientId);
        return contactRepository.searchContacts(clientId, organizationId, keyword.trim(), pageable)
                .map(contactMapper::toResponse);
    }

    /**
     * Update the profile of a contact.
     *
     * @param organizationId authenticated tenant id
     * @param clientId       owning client id
     * @param contactId      requested contact id
     * @param request        update payload
     * @return updated contact
     */
    @Transactional
    public ClientContactResponse updateContact(Long organizationId, Long clientId, Long contactId,
                                               UpdateClientContactRequest request) {
        ClientContact contact = findContactInOrganization(organizationId, clientId, contactId);
        contactMapper.updateEntity(contact, request);
        return contactMapper.toResponse(contactRepository.save(contact));
    }

    /**
     * Activate a contact without touching the deleted flag.
     *
     * @param organizationId authenticated tenant id
     * @param clientId       owning client id
     * @param contactId      requested contact id
     * @return activated contact
     */
    @Transactional
    public ClientContactResponse activateContact(Long organizationId, Long clientId, Long contactId) {
        ClientContact contact = findContactInOrganization(organizationId, clientId, contactId);
        contact.setActive(true);
        return contactMapper.toResponse(contactRepository.save(contact));
    }

    /**
     * Deactivate a contact without deleting it.
     *
     * @param organizationId authenticated tenant id
     * @param clientId       owning client id
     * @param contactId      requested contact id
     * @return deactivated contact
     */
    @Transactional
    public ClientContactResponse deactivateContact(Long organizationId, Long clientId, Long contactId) {
        ClientContact contact = findContactInOrganization(organizationId, clientId, contactId);
        contact.setActive(false);
        return contactMapper.toResponse(contactRepository.save(contact));
    }

    /**
     * Soft delete a contact. The record is never physically removed.
     *
     * @param organizationId authenticated tenant id
     * @param clientId       owning client id
     * @param contactId      requested contact id
     */
    @Transactional
    public void deleteContact(Long organizationId, Long clientId, Long contactId) {
        ClientContact contact = findContactInOrganization(organizationId, clientId, contactId);
        contact.softDelete();
        contactRepository.save(contact);
    }

    /**
     * Load a non deleted contact and verify both that it belongs to the
     * authenticated organization and to the addressed client. A contact of
     * another organization or another client is forbidden, a missing or
     * soft-deleted contact is not found.
     *
     * @param organizationId authenticated tenant id
     * @param clientId       owning client id
     * @param contactId      requested contact id
     * @return matching contact
     */
    private ClientContact findContactInOrganization(Long organizationId, Long clientId, Long contactId) {
        ClientContact contact = contactRepository.findByIdAndNotDeleted(contactId)
                .orElseThrow(() -> new ClientContactNotFoundException(contactId));
        boolean belongsToTenant = contactRepository.existsByIdAndOrganization(contactId, organizationId);
        if (!belongsToTenant || !contact.getClientId().equals(clientId)) {
            throw new ClientContactAccessDeniedException();
        }
        return contact;
    }
}