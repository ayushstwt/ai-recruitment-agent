package com.ayshriv.recruitment.clientContact.service;

import com.ayshriv.recruitment.client.entity.Client;
import com.ayshriv.recruitment.client.service.ClientService;
import com.ayshriv.recruitment.clientContact.dto.request.CreateClientContactRequest;
import com.ayshriv.recruitment.clientContact.dto.request.UpdateClientContactRequest;
import com.ayshriv.recruitment.clientContact.dto.response.ClientContactResponse;
import com.ayshriv.recruitment.clientContact.entity.ClientContact;
import com.ayshriv.recruitment.clientContact.exception.ClientContactAccessDeniedException;
import com.ayshriv.recruitment.clientContact.exception.ClientContactNotFoundException;
import com.ayshriv.recruitment.clientContact.mapper.ClientContactMapper;
import com.ayshriv.recruitment.clientContact.repository.ClientContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientContactServiceTest {

    @Mock
    private ClientContactRepository contactRepository;

    @Mock
    private ClientService clientService;

    @Spy
    private ClientContactMapper contactMapper = new ClientContactMapper();

    private ClientContactService contactService;

    @BeforeEach
    void setUp() {
        contactService = new ClientContactService(contactRepository, contactMapper, clientService);
    }

    private Client client(Long id, Long organizationId) {
        Client client = new Client(id);
        client.setOrganization(new com.ayshriv.recruitment.organization.entity.Organization(organizationId));
        return client;
    }

    private ClientContact contact(Long id, Long clientId, Long organizationId) {
        ClientContact contact = new ClientContact();
        contact.setId(id);
        contact.setFirstName("Sarah");
        contact.setLastName("Smith");
        contact.setEmail("sarah@acme.com");
        contact.setPhone("+911234567890");
        contact.setJobTitle("HR Manager");
        contact.setDepartment("Human Resources");
        contact.setNotes("Primary hiring contact");
        contact.setClient(client(clientId, organizationId));
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

    private UpdateClientContactRequest updateRequest() {
        return new UpdateClientContactRequest(
                "Sara", "Smith", "sara@acme.com", "+910987654321",
                "Senior HR Manager", "People Operations", "Senior contact");
    }

    @Test
    void createContactAssignsVerifiedClient() {
        when(clientService.getClientEntity(10L, 5L)).thenReturn(client(5L, 10L));
        when(contactRepository.save(any(ClientContact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientContactResponse response = contactService.createContact(10L, 5L, createRequest());

        ArgumentCaptor<ClientContact> captor = ArgumentCaptor.forClass(ClientContact.class);
        verify(contactRepository).save(captor.capture());
        ClientContact saved = captor.getValue();

        assertThat(response.getFirstName()).isEqualTo("Sarah");
        assertThat(saved.getClientId()).isEqualTo(5L);
        verify(clientService).getClientEntity(10L, 5L);
    }

    @Test
    void getContactReturnsMappedContact() {
        ClientContact contact = contact(10L, 5L, 10L);
        when(contactRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(contact));
        when(contactRepository.existsByIdAndOrganization(10L, 10L)).thenReturn(true);

        ClientContactResponse response = contactService.getContact(10L, 5L, 10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getEmail()).isEqualTo("sarah@acme.com");
        assertThat(response.getJobTitle()).isEqualTo("HR Manager");
    }

    @Test
    void getContactThrowsNotFound() {
        when(contactRepository.findByIdAndNotDeleted(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> contactService.getContact(10L, 5L, 99L))
                .isInstanceOf(ClientContactNotFoundException.class)
                .extracting(ex -> ((ClientContactNotFoundException) ex).getCode())
                .isEqualTo("CLIENT_CONTACT_NOT_FOUND");
    }

    @Test
    void getContactThrowsForbiddenForCrossOrganizationContact() {
        when(contactRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(contact(10L, 5L, 20L)));
        when(contactRepository.existsByIdAndOrganization(10L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> contactService.getContact(10L, 5L, 10L))
                .isInstanceOf(ClientContactAccessDeniedException.class)
                .extracting(ex -> ((ClientContactAccessDeniedException) ex).getCode())
                .isEqualTo("CLIENT_CONTACT_ACCESS_DENIED");
    }

    @Test
    void getContactThrowsForbiddenWhenContactBelongsToAnotherClient() {
        when(contactRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(contact(10L, 7L, 10L)));
        when(contactRepository.existsByIdAndOrganization(10L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> contactService.getContact(10L, 5L, 10L))
                .isInstanceOf(ClientContactAccessDeniedException.class)
                .extracting(ex -> ((ClientContactAccessDeniedException) ex).getCode())
                .isEqualTo("CLIENT_CONTACT_ACCESS_DENIED");
    }

    @Test
    void getContactsVerifiesClientAndDelegatesToScopedQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        when(clientService.getClientEntity(10L, 5L)).thenReturn(client(5L, 10L));
        when(contactRepository.findAllByClientIdAndOrganization(5L, 10L, pageable))
                .thenReturn(new PageImpl<>(List.of(contact(10L, 5L, 10L)), pageable, 1));

        Page<ClientContactResponse> result = contactService.getContacts(10L, 5L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getDepartment()).isEqualTo("Human Resources");
        verify(clientService).getClientEntity(10L, 5L);
        verify(contactRepository).findAllByClientIdAndOrganization(5L, 10L, pageable);
    }

    @Test
    void searchContactsDelegatesToOrganizationScopedQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        when(clientService.getClientEntity(10L, 5L)).thenReturn(client(5L, 10L));
        when(contactRepository.searchContacts(eq(5L), eq(10L), eq("sarah"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(contact(10L, 5L, 10L)), pageable, 1));

        Page<ClientContactResponse> result = contactService.searchContacts(10L, 5L, "  sarah  ", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(contactRepository).searchContacts(5L, 10L, "sarah", pageable);
    }

    @Test
    void searchContactsWithBlankKeywordFallsBackToAllContacts() {
        Pageable pageable = PageRequest.of(0, 20);
        when(clientService.getClientEntity(10L, 5L)).thenReturn(client(5L, 10L));
        when(contactRepository.findAllByClientIdAndOrganization(5L, 10L, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        contactService.searchContacts(10L, 5L, "   ", pageable);

        verify(contactRepository).findAllByClientIdAndOrganization(5L, 10L, pageable);
        verify(contactRepository, never()).searchContacts(any(), any(), any(), any());
    }

    @Test
    void updateContactAppliesProfileChanges() {
        ClientContact contact = contact(10L, 5L, 10L);
        when(contactRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(contact));
        when(contactRepository.existsByIdAndOrganization(10L, 10L)).thenReturn(true);
        when(contactRepository.save(any(ClientContact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientContactResponse response = contactService.updateContact(10L, 5L, 10L, updateRequest());

        assertThat(response.getFirstName()).isEqualTo("Sara");
        assertThat(response.getEmail()).isEqualTo("sara@acme.com");
        assertThat(response.getJobTitle()).isEqualTo("Senior HR Manager");
        assertThat(contact.getClientId()).isEqualTo(5L);
        assertThat(contact.isActive()).isTrue();
        assertThat(contact.isDeleted()).isFalse();
    }

    @Test
    void activateContactActivatesWithoutDeleting() {
        ClientContact contact = contact(10L, 5L, 10L);
        contact.setActive(false);
        when(contactRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(contact));
        when(contactRepository.existsByIdAndOrganization(10L, 10L)).thenReturn(true);
        when(contactRepository.save(any(ClientContact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientContactResponse response = contactService.activateContact(10L, 5L, 10L);

        assertThat(response.isActive()).isTrue();
        assertThat(contact.isActive()).isTrue();
        assertThat(contact.isDeleted()).isFalse();
    }

    @Test
    void deactivateContactDeactivatesWithoutDeleting() {
        ClientContact contact = contact(10L, 5L, 10L);
        when(contactRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(contact));
        when(contactRepository.existsByIdAndOrganization(10L, 10L)).thenReturn(true);
        when(contactRepository.save(any(ClientContact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientContactResponse response = contactService.deactivateContact(10L, 5L, 10L);

        assertThat(response.isActive()).isFalse();
        assertThat(contact.isDeleted()).isFalse();
    }

    @Test
    void deleteContactSoftDeletes() {
        ClientContact contact = contact(10L, 5L, 10L);
        when(contactRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(contact));
        when(contactRepository.existsByIdAndOrganization(10L, 10L)).thenReturn(true);
        when(contactRepository.save(any(ClientContact.class))).thenAnswer(invocation -> invocation.getArgument(0));

        contactService.deleteContact(10L, 5L, 10L);

        assertThat(contact.isDeleted()).isTrue();
        assertThat(contact.isActive()).isFalse();
        verify(contactRepository).save(contact);
        verify(contactRepository, never()).delete(any());
    }

    @Test
    void deleteContactThrowsForbiddenForCrossOrganizationContact() {
        when(contactRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(contact(10L, 5L, 20L)));
        when(contactRepository.existsByIdAndOrganization(10L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> contactService.deleteContact(10L, 5L, 10L))
                .isInstanceOf(ClientContactAccessDeniedException.class)
                .extracting(ex -> ((ClientContactAccessDeniedException) ex).getCode())
                .isEqualTo("CLIENT_CONTACT_ACCESS_DENIED");
        verify(contactRepository, never()).save(any());
    }
}