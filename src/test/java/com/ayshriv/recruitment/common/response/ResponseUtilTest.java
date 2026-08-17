package com.ayshriv.recruitment.common.response;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseUtilTest {

    @Test
    void successBuildsEnvelopeWithoutMetadata() {
        ApiResponse<Map<String, Object>> response =
                ResponseUtil.success("Candidate retrieved successfully", Map.of("id", 101), "/api/v1/candidates/101");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Candidate retrieved successfully");
        assertThat(response.getData()).containsEntry("id", 101);
        assertThat(response.getMetadata()).isNull();
        assertThat(response.getError()).isNull();
        assertThat(response.getPath()).isEqualTo("/api/v1/candidates/101");
        assertThat(response.getTimestamp()).isInstanceOf(OffsetDateTime.class);
    }

    @Test
    void successBuildsEnvelopeWithMetadata() {
        Map<String, Object> metadata = Map.of("model", "azure-openai", "candidateCount", 25);
        ApiResponse<String> response = ResponseUtil.success(
                "Candidate matching completed successfully", "payload", metadata, "/api/v1/ai/recruitment/match");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("payload");
        assertThat(response.getMetadata()).isEqualTo(metadata);
        assertThat(response.getError()).isNull();
        assertThat(response.getPath()).isEqualTo("/api/v1/ai/recruitment/match");
    }

    @Test
    void errorBuildsEnvelopeWithCodeAndDetails() {
        Map<String, String> details = Map.of("email", "Invalid email address");
        ApiResponse<Object> response = ResponseUtil.error(
                "Request validation failed", "VALIDATION_ERROR", details, "/api/v1/candidates");

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("Request validation failed");
        assertThat(response.getData()).isNull();
        assertThat(response.getMetadata()).isNull();
        assertThat(response.getError().getCode()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getError().getDetails()).isEqualTo(details);
        assertThat(response.getPath()).isEqualTo("/api/v1/candidates");
    }

    @Test
    void successPageConvertsSpringPageIntoListAndMetadata() {
        Page<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 2), 125);

        ApiResponse<List<String>> response =
                ResponseUtil.successPage("Candidates retrieved successfully", page, "/api/v1/candidates");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).containsExactly("a", "b");
        assertThat(response.getError()).isNull();
        assertThat(response.getPath()).isEqualTo("/api/v1/candidates");

        assertThat(response.getMetadata()).isInstanceOf(PageMetadata.class);
        PageMetadata metadata = (PageMetadata) response.getMetadata();
        assertThat(metadata.getPage()).isZero();
        assertThat(metadata.getSize()).isEqualTo(2);
        assertThat(metadata.getTotalElements()).isEqualTo(125);
        assertThat(metadata.getTotalPages()).isEqualTo(63);
        assertThat(metadata.isHasNext()).isTrue();
        assertThat(metadata.isHasPrevious()).isFalse();
    }
}