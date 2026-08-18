package com.ayshriv.recruitment.client.service;

import com.ayshriv.recruitment.client.dto.request.CreateClientRequest;
import com.ayshriv.recruitment.client.dto.request.UpdateClientRequest;
import com.ayshriv.recruitment.client.dto.response.ClientResponse;
import com.ayshriv.recruitment.client.dto.response.ClientSummaryResponse;
import com.ayshriv.recruitment.client.entity.Client;
import com.ayshriv.recruitment.client.entity.CompanySize;
import com.ayshriv.recruitment.client.exception.ClientAccessDeniedException;
import com.ayshriv.recruitment.client.exception.ClientNotFoundException;
import com.ayshriv.recruitment.client.exception.DuplicateClientException;
import com.ayshriv.recruitment.client.mapper.ClientMapper;
import com.ayshriv.recruitment.client.repository.ClientRepository;
import com.ayshriv.recruitment.common.exception.BadRequestException;
import com.ayshriv.recruitment.organization.entity.Organization;
import com.ayshriv.recruitment.organization.exception.OrganizationNotFoundException;
import com.ayshriv.recruitment.organization.repository.OrganizationRepository;
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
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private ClientCodeGenerator clientCodeGenerator;

    @Spy
    private ClientMapper clientMapper = new ClientMapper();

    private ClientService clientService;

    @BeforeEach
    void setUp() {
        clientService = new ClientService(clientRepository, organizationRepository, clientMapper,
                clientCodeGenerator);
    }

    private Client client(Long id, Long organizationId) {
        Client client = new Client();
        client.setId(id);
        client.setClientCode("CLI-" + String.format("%06d", id));
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
        client.setOrganization(new Organization(organizationId));
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

    private UpdateClientRequest updateRequest() {
        return new UpdateClientRequest(
                "Acme Global", "Acme Global Pvt Ltd", "careers@acme.com", "+910987654321",
                "https://www.acme.com", "Enterprise Software", CompanySize.LARGE, "India", "Maharashtra",
                "Mumbai", "Tower A", "400001", "Asia/Kolkata",
                "Global product company", "Strategic account");
    }

    @Test
    void createClientAssignsOrganizationAndGeneratedCode() {
        when(clientRepository.findByEmailAndOrganization(eq("hr@acme.com"), eq(10L)))
                .thenReturn(Optional.empty());
        when(organizationRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(new Organization(10L)));
        when(clientCodeGenerator.nextCode(10L)).thenReturn("CLI-000001");
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientResponse response = clientService.createClient(10L, createRequest());

        ArgumentCaptor<Client> captor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(captor.capture());
        Client saved = captor.getValue();

        assertThat(response.getClientCode()).isEqualTo("CLI-000001");
        assertThat(saved.getClientCode()).isEqualTo("CLI-000001");
        assertThat(saved.getOrganizationId()).isEqualTo(10L);
        assertThat(saved.getCompanyName()).isEqualTo("Acme Technologies");
        assertThat(saved.getCompanySize()).isEqualTo(CompanySize.MEDIUM);
        verify(organizationRepository).findByIdForUpdate(10L);
    }

    @Test
    void createClientLocksTheOrganizationBeforeCodeAllocation() {
        when(clientRepository.findByEmailAndOrganization(any(), eq(10L))).thenReturn(Optional.empty());
        when(organizationRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.createClient(10L, createRequest()))
                .isInstanceOf(OrganizationNotFoundException.class);
        verify(clientRepository, never()).save(any());
        verify(clientCodeGenerator, never()).nextCode(10L);
    }

    @Test
    void createClientRejectsDuplicateEmailWithinOrganization() {
        when(clientRepository.findByEmailAndOrganization("hr@acme.com", 10L))
                .thenReturn(Optional.of(client(5L, 10L)));

        assertThatThrownBy(() -> clientService.createClient(10L, createRequest()))
                .isInstanceOf(DuplicateClientException.class)
                .extracting(ex -> ((DuplicateClientException) ex).getCode())
                .isEqualTo("CLIENT_ALREADY_EXISTS");
        verify(clientRepository, never()).save(any());
    }

    @Test
    void createClientRejectsInvalidTimezone() {
        CreateClientRequest request = createRequest();
        request.setTimezone("Not/AZone");

        assertThatThrownBy(() -> clientService.createClient(10L, request))
                .isInstanceOf(BadRequestException.class)
                .extracting(ex -> ((BadRequestException) ex).getCode())
                .isEqualTo("INVALID_TIMEZONE");
        verify(clientRepository, never()).save(any());
    }

    @Test
    void createClientResponseNeverExposesEntityOrOrganization() {
        when(clientRepository.findByEmailAndOrganization(eq("hr@acme.com"), eq(10L)))
                .thenReturn(Optional.empty());
        when(organizationRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(new Organization(10L)));
        when(clientCodeGenerator.nextCode(10L)).thenReturn("CLI-000001");
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientResponse response = clientService.createClient(10L, createRequest());

        assertThat(response).isNotInstanceOf(Client.class);
        assertThat(hasField(response, "organization")).isFalse();
        assertThat(hasField(response, "contacts")).isFalse();
        assertThat(hasField(response, "clientCode")).isTrue();
    }

    @Test
    void getClientByIdReturnsMappedClient() {
        when(clientRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(client(10L, 10L)));

        ClientResponse response = clientService.getClientById(10L, 10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getCompanyName()).isEqualTo("Acme Technologies");
        assertThat(response.getClientCode()).isEqualTo("CLI-000010");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void getClientByIdThrowsNotFound() {
        when(clientRepository.findByIdAndNotDeleted(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.getClientById(10L, 99L))
                .isInstanceOf(ClientNotFoundException.class)
                .extracting(ex -> ((ClientNotFoundException) ex).getCode())
                .isEqualTo("CLIENT_NOT_FOUND");
    }

    @Test
    void getClientByIdThrowsForbiddenWhenClientBelongsToAnotherOrganization() {
        when(clientRepository.findByIdAndNotDeleted(2L)).thenReturn(Optional.of(client(2L, 20L)));

        assertThatThrownBy(() -> clientService.getClientById(10L, 2L))
                .isInstanceOf(ClientAccessDeniedException.class)
                .extracting(ex -> ((ClientAccessDeniedException) ex).getCode())
                .isEqualTo("CLIENT_ACCESS_DENIED");
    }

    @Test
    void getClientsReturnsMappedPageScopedToOrganization() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Client> page = new PageImpl<>(List.of(client(10L, 10L)), pageable, 1);
        when(clientRepository.findAllByOrganization(10L, pageable)).thenReturn(page);

        Page<ClientResponse> result = clientService.getClients(10L, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCompanyName()).isEqualTo("Acme Technologies");
        verify(clientRepository).findAllByOrganization(10L, pageable);
    }

    @Test
    void searchClientsDelegatesToOrganizationScopedQuery() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Client> page = new PageImpl<>(List.of(client(10L, 10L)), pageable, 1);
        when(clientRepository.searchClients(eq(10L), eq("acme"), eq(pageable))).thenReturn(page);

        Page<ClientSummaryResponse> result = clientService.searchClients(10L, "  acme  ", pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getClientCode()).isEqualTo("CLI-000010");
        assertThat(result.getContent().get(0).getIndustry()).isEqualTo("Technology");
        verify(clientRepository).searchClients(10L, "acme", pageable);
    }

    @Test
    void searchClientsWithBlankKeywordFallsBackToAllClients() {
        Pageable pageable = PageRequest.of(0, 20);
        when(clientRepository.findAllByOrganization(10L, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        clientService.searchClients(10L, "   ", pageable);

        verify(clientRepository).findAllByOrganization(10L, pageable);
        verify(clientRepository, never()).searchClients(any(), any(), any());
    }

    @Test
    void updateClientAppliesProfileChanges() {
        Client client = client(10L, 10L);
        when(clientRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(client));
        when(clientRepository.findByEmailAndOrganization("careers@acme.com", 10L)).thenReturn(Optional.empty());
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientResponse response = clientService.updateClient(10L, 10L, updateRequest());

        assertThat(response.getCompanyName()).isEqualTo("Acme Global");
        assertThat(response.getEmail()).isEqualTo("careers@acme.com");
        assertThat(response.getCompanySize()).isEqualTo(CompanySize.LARGE);
        assertThat(response.getClientCode()).isEqualTo("CLI-000010");
        assertThat(client.isActive()).isTrue();
        assertThat(client.isDeleted()).isFalse();
    }

    @Test
    void updateClientAllowsKeepingOwnEmail() {
        Client client = client(10L, 10L);
        when(clientRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(client));
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateClientRequest request = updateRequest();
        request.setEmail("hr@acme.com");

        ClientResponse response = clientService.updateClient(10L, 10L, request);

        assertThat(response.getEmail()).isEqualTo("hr@acme.com");
        verify(clientRepository, never()).findByEmailAndOrganization(any(), any());
    }

    @Test
    void updateClientRejectsEmailUsedByAnotherClient() {
        when(clientRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(client(10L, 10L)));
        when(clientRepository.findByEmailAndOrganization("taken@acme.com", 10L))
                .thenReturn(Optional.of(client(11L, 10L)));

        UpdateClientRequest request = updateRequest();
        request.setEmail("taken@acme.com");

        assertThatThrownBy(() -> clientService.updateClient(10L, 10L, request))
                .isInstanceOf(DuplicateClientException.class)
                .extracting(ex -> ((DuplicateClientException) ex).getCode())
                .isEqualTo("CLIENT_ALREADY_EXISTS");
        verify(clientRepository, never()).save(any());
    }

    @Test
    void updateClientThrowsForbiddenWhenTargetBelongsToAnotherOrganization() {
        when(clientRepository.findByIdAndNotDeleted(2L)).thenReturn(Optional.of(client(2L, 20L)));

        assertThatThrownBy(() -> clientService.updateClient(10L, 2L, updateRequest()))
                .isInstanceOf(ClientAccessDeniedException.class)
                .extracting(ex -> ((ClientAccessDeniedException) ex).getCode())
                .isEqualTo("CLIENT_ACCESS_DENIED");
    }

    @Test
    void activateClientActivatesWithoutDeleting() {
        Client client = client(10L, 10L);
        client.setActive(false);
        when(clientRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(client));
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientResponse response = clientService.activateClient(10L, 10L);

        assertThat(response.isActive()).isTrue();
        assertThat(client.isActive()).isTrue();
        assertThat(client.isDeleted()).isFalse();
    }

    @Test
    void deactivateClientDeactivatesWithoutDeleting() {
        Client client = client(10L, 10L);
        when(clientRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(client));
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientResponse response = clientService.deactivateClient(10L, 10L);

        assertThat(response.isActive()).isFalse();
        assertThat(client.isDeleted()).isFalse();
    }

    @Test
    void deactivateClientThrowsForbiddenForAnotherOrganization() {
        when(clientRepository.findByIdAndNotDeleted(2L)).thenReturn(Optional.of(client(2L, 20L)));

        assertThatThrownBy(() -> clientService.deactivateClient(10L, 2L))
                .isInstanceOf(ClientAccessDeniedException.class)
                .extracting(ex -> ((ClientAccessDeniedException) ex).getCode())
                .isEqualTo("CLIENT_ACCESS_DENIED");
    }

    @Test
    void deleteClientSoftDeletes() {
        Client client = client(10L, 10L);
        when(clientRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(client));
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        clientService.deleteClient(10L, 10L);

        assertThat(client.isDeleted()).isTrue();
        assertThat(client.isActive()).isFalse();
        verify(clientRepository).save(client);
        verify(clientRepository, never()).delete(any(Client.class));
    }

    @Test
    void deleteClientThrowsForbiddenForAnotherOrganization() {
        when(clientRepository.findByIdAndNotDeleted(2L)).thenReturn(Optional.of(client(2L, 20L)));

        assertThatThrownBy(() -> clientService.deleteClient(10L, 2L))
                .isInstanceOf(ClientAccessDeniedException.class)
                .extracting(ex -> ((ClientAccessDeniedException) ex).getCode())
                .isEqualTo("CLIENT_ACCESS_DENIED");
        verify(clientRepository, never()).save(any());
    }

    @Test
    void getClientEntityVerifiesTenantOwnership() {
        when(clientRepository.findByIdAndNotDeleted(10L)).thenReturn(Optional.of(client(10L, 10L)));

        Client entity = clientService.getClientEntity(10L, 10L);

        assertThat(entity.getId()).isEqualTo(10L);
    }

    @Test
    void getClientEntityThrowsForbiddenForAnotherOrganization() {
        when(clientRepository.findByIdAndNotDeleted(2L)).thenReturn(Optional.of(client(2L, 20L)));

        assertThatThrownBy(() -> clientService.getClientEntity(10L, 2L))
                .isInstanceOf(ClientAccessDeniedException.class);
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