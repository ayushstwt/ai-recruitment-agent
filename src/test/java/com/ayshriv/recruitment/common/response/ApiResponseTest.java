package com.ayshriv.recruitment.common.response;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void builderPopulatesEveryField() {
        ApiError error = new ApiError("CANDIDATE_NOT_FOUND", null);
        OffsetDateTime timestamp = OffsetDateTime.parse("2026-08-16T22:30:00+05:30");

        ApiResponse<String> response = ApiResponse.<String>builder()
                .success(false)
                .message("Candidate not found")
                .data(null)
                .metadata(null)
                .error(error)
                .timestamp(timestamp)
                .path("/api/v1/candidates/999")
                .build();

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Candidate not found");
        assertThat(response.getData()).isNull();
        assertThat(response.getMetadata()).isNull();
        assertThat(response.getError()).isSameAs(error);
        assertThat(response.getError().getCode()).isEqualTo("CANDIDATE_NOT_FOUND");
        assertThat(response.getTimestamp()).isEqualTo(timestamp);
        assertThat(response.getPath()).isEqualTo("/api/v1/candidates/999");
    }

    @Test
    void supportsEmptyInstantiation() {
        ApiResponse<String> response = new ApiResponse<>();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getError()).isNull();
    }
}