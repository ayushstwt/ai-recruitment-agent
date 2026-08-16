package com.ayshriv.recruitment.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceNotFoundExceptionTest {

    @Test
    void usesDefaultCodeWhenNotProvided() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Candidate not found");

        assertThat(ex.getMessage()).isEqualTo("Candidate not found");
        assertThat(ex.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void usesCustomCodeWhenProvided() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Candidate not found", "CANDIDATE_NOT_FOUND");

        assertThat(ex.getCode()).isEqualTo("CANDIDATE_NOT_FOUND");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}