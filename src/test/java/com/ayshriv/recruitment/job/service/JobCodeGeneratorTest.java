package com.ayshriv.recruitment.job.service;

import com.ayshriv.recruitment.job.repository.JobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobCodeGeneratorTest {

    @Mock
    private JobRepository jobRepository;

    private JobCodeGenerator codeGenerator;

    @BeforeEach
    void setUp() {
        codeGenerator = new JobCodeGenerator(jobRepository);
    }

    @Test
    void firstCodeIsJob000001() {
        when(jobRepository.countByOrganization(10L)).thenReturn(0L);

        assertThat(codeGenerator.nextCode(10L)).isEqualTo("JOB-000001");
    }

    @Test
    void nextCodeContinuesSequentially() {
        when(jobRepository.countByOrganization(10L)).thenReturn(9L);

        assertThat(codeGenerator.nextCode(10L)).isEqualTo("JOB-000010");
    }

    @Test
    void codesArePaddedToSixDigits() {
        when(jobRepository.countByOrganization(10L)).thenReturn(999_999L);

        assertThat(codeGenerator.nextCode(10L)).isEqualTo("JOB-1000000");
    }

    @Test
    void codeAllocationIsScopedPerOrganization() {
        when(jobRepository.countByOrganization(10L)).thenReturn(1L);
        when(jobRepository.countByOrganization(20L)).thenReturn(4L);

        assertThat(codeGenerator.nextCode(10L)).isEqualTo("JOB-000002");
        assertThat(codeGenerator.nextCode(20L)).isEqualTo("JOB-000005");
    }
}
