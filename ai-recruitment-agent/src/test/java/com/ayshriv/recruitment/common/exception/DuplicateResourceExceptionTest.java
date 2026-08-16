package com.ayshriv.recruitment.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateResourceExceptionTest {

    @Test
    void usesDefaultCodeWhenNotProvided() {
        DuplicateResourceException ex = new DuplicateResourceException("Candidate already exists");

        assertThat(ex.getMessage()).isEqualTo("Candidate already exists");
        assertThat(ex.getCode()).isEqualTo("DUPLICATE_RESOURCE");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void usesCustomCodeWhenProvided() {
        DuplicateResourceException ex =
                new DuplicateResourceException("Candidate already exists", "CANDIDATE_ALREADY_EXISTS");

        assertThat(ex.getCode()).isEqualTo("CANDIDATE_ALREADY_EXISTS");
        assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }
}