package com.ayshriv.recruitment.client.service;

import com.ayshriv.recruitment.client.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientCodeGeneratorTest {

    @Mock
    private ClientRepository clientRepository;

    private ClientCodeGenerator codeGenerator;

    @BeforeEach
    void setUp() {
        codeGenerator = new ClientCodeGenerator(clientRepository);
    }

    @Test
    void firstCodeIsCli000001() {
        when(clientRepository.countByOrganization(10L)).thenReturn(0L);

        assertThat(codeGenerator.nextCode(10L)).isEqualTo("CLI-000001");
    }

    @Test
    void nextCodeContinuesSequentially() {
        when(clientRepository.countByOrganization(10L)).thenReturn(9L);

        assertThat(codeGenerator.nextCode(10L)).isEqualTo("CLI-000010");
    }

    @Test
    void codesArePaddedToSixDigits() {
        when(clientRepository.countByOrganization(10L)).thenReturn(999_999L);

        assertThat(codeGenerator.nextCode(10L)).isEqualTo("CLI-1000000");
    }

    @Test
    void codeAllocationIsScopedPerOrganization() {
        when(clientRepository.countByOrganization(10L)).thenReturn(1L);
        when(clientRepository.countByOrganization(20L)).thenReturn(4L);

        assertThat(codeGenerator.nextCode(10L)).isEqualTo("CLI-000002");
        assertThat(codeGenerator.nextCode(20L)).isEqualTo("CLI-000005");
    }
}