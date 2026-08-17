package com.ayshriv.recruitment.common.exception;

import com.ayshriv.recruitment.common.response.ApiResponse;
import com.ayshriv.recruitment.common.response.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @RestController
    @RequestMapping("/api/v1/test")
    static class TestController {

        @GetMapping("/not-found")
        public ResponseEntity<ApiResponse<Object>> notFound(HttpServletRequest request) {
            throw new ResourceNotFoundException("Candidate not found", "CANDIDATE_NOT_FOUND");
        }

        @GetMapping("/bad-request")
        public ResponseEntity<ApiResponse<Object>> badRequest(HttpServletRequest request) {
            throw new BadRequestException("Invalid payload");
        }

        @GetMapping("/unauthorized")
        public ResponseEntity<ApiResponse<Object>> unauthorized(HttpServletRequest request) {
            throw new UnauthorizedException("Authentication required");
        }

        @GetMapping("/forbidden")
        public ResponseEntity<ApiResponse<Object>> forbidden(HttpServletRequest request) {
            throw new ForbiddenException("You do not have permission to perform this action");
        }

        @GetMapping("/duplicate")
        public ResponseEntity<ApiResponse<Object>> duplicate(HttpServletRequest request) {
            throw new DuplicateResourceException("Candidate already exists", "CANDIDATE_ALREADY_EXISTS");
        }

        @GetMapping("/data-integrity")
        public ResponseEntity<ApiResponse<Object>> dataIntegrity(HttpServletRequest request) {
            throw new DataIntegrityViolationException("constraint violation");
        }

        @GetMapping("/generic")
        public ResponseEntity<ApiResponse<Object>> generic(HttpServletRequest request) {
            throw new IllegalStateException("internal boom");
        }

        @PostMapping("/validate")
        public ResponseEntity<ApiResponse<Object>> validate(
                @Valid @RequestBody TestRequest body, HttpServletRequest request) {
            return ResponseEntity.ok(ResponseUtil.success("Valid", body, request.getRequestURI()));
        }

        @GetMapping("/success")
        public ResponseEntity<ApiResponse<Map<String, Integer>>> success(HttpServletRequest request) {
            return ResponseEntity.ok(ResponseUtil.success(
                    "Candidate retrieved successfully", Map.of("id", 101), request.getRequestURI()));
        }

        @GetMapping("/page")
        public ResponseEntity<ApiResponse<List<String>>> page(HttpServletRequest request) {
            Page<String> page = new PageImpl<>(List.of("a", "b"), PageRequest.of(0, 20), 125);
            return ResponseEntity.ok(ResponseUtil.successPage("Candidates retrieved successfully", page,
                    request.getRequestURI()));
        }
    }

    static class TestRequest {
        @NotBlank(message = "First name is required")
        private String firstName;

        @Email(message = "Invalid email address")
        private String email;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    @Test
    void notFoundReturnsConsistent404Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Candidate not found"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.metadata").value(nullValue()))
                .andExpect(jsonPath("$.error.code").value("CANDIDATE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.details").value(nullValue()))
                .andExpect(jsonPath("$.path").value("/api/v1/test/not-found"));
    }

    @Test
    void badRequestReturns400Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/test/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void unauthorizedReturns401Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/test/unauthorized"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void forbiddenReturns403Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/test/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void duplicateResourceReturns409EnvelopeWithCustomCode() throws Exception {
        mockMvc.perform(get("/api/v1/test/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("CANDIDATE_ALREADY_EXISTS"));
    }

    @Test
    void dataIntegrityReturns409Envelope() throws Exception {
        mockMvc.perform(get("/api/v1/test/data-integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DATA_INTEGRITY_VIOLATION"));
    }

    @Test
    void genericExceptionReturns500EnvelopeWithoutInternals() throws Exception {
        mockMvc.perform(get("/api/v1/test/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.error.details").value(nullValue()))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void validationErrorCollectsFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details.firstName").value("First name is required"))
                .andExpect(jsonPath("$.error.details.email").value("Invalid email address"))
                .andExpect(jsonPath("$.path").value("/api/v1/test/validate"));
    }

    @Test
    void malformedBodyReturns400Envelope() throws Exception {
        mockMvc.perform(post("/api/v1/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Malformed request body"))
                .andExpect(jsonPath("$.error.code").value("BAD_REQUEST"));
    }

    @Test
    void successResponseSerializesFullContract() throws Exception {
        String body = mockMvc.perform(get("/api/v1/test/success"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("\"success\":true");
        assertThat(body).contains("\"message\":\"Candidate retrieved successfully\"");
        assertThat(body).contains("\"data\"");
        assertThat(body).contains("\"id\":101");
        assertThat(body).contains("\"metadata\":null");
        assertThat(body).contains("\"error\":null");
        assertThat(body).contains("\"path\":\"/api/v1/test/success\"");
        assertThat(body)
                .matches(".*\"timestamp\":\"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+\\d{2}:\\d{2}\".*");
    }

    @Test
    void errorResponseSerializesFullContract() throws Exception {
        String body = mockMvc.perform(get("/api/v1/test/not-found"))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("\"success\":false");
        assertThat(body).contains("\"data\":null");
        assertThat(body).contains("\"metadata\":null");
        assertThat(body).contains("\"error\":{\"code\":\"CANDIDATE_NOT_FOUND\",\"details\":null}");
        assertThat(body).contains("\"path\":\"/api/v1/test/not-found\"");
        assertThat(body)
                .matches(".*\"timestamp\":\"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+\\d{2}:\\d{2}\".*");
    }

    @Test
    void paginatedResponseSerializesMetadata() throws Exception {
        mockMvc.perform(get("/api/v1/test/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0]").value("a"))
                .andExpect(jsonPath("$.data[1]").value("b"))
                .andExpect(jsonPath("$.metadata.page").value(0))
                .andExpect(jsonPath("$.metadata.size").value(20))
                .andExpect(jsonPath("$.metadata.totalElements").value(125))
                .andExpect(jsonPath("$.metadata.totalPages").value(7))
                .andExpect(jsonPath("$.metadata.hasNext").value(true))
                .andExpect(jsonPath("$.metadata.hasPrevious").value(false))
                .andExpect(jsonPath("$.error").value(nullValue()));
    }
}