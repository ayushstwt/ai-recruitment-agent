package com.ayshriv.recruitment.common.exception;

import com.ayshriv.recruitment.common.response.ApiResponse;
import com.ayshriv.recruitment.common.response.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central place where every exception is translated into a consistent
 * {@link ApiResponse} envelope.
 *
 * <p>API consumers never see random JSON structures or raw exception
 * class names, only the standard response contract defined in the
 * {@code common.response} package.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle business exceptions raised through {@link ApiException}.
     *
     * @param ex      the caught exception
     * @param request current request, used to populate the response path
     * @return error response with the status carried by the exception
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleApiException(ApiException ex, HttpServletRequest request) {
        return ResponseEntity.status(ex.getStatus())
                .body(ResponseUtil.error(ex.getMessage(), ex.getCode(), null, request.getRequestURI()));
    }

    /**
     * Handle request body validation failures triggered by Jakarta Bean Validation.
     *
     * @param ex      the caught exception
     * @param request current request
     * @return bad request response with field level validation details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseUtil.error(
                        "Request validation failed", "VALIDATION_ERROR", fieldErrors, request.getRequestURI()));
    }

    /**
     * Handle constraint violations raised outside of request body binding.
     *
     * @param ex      the caught exception
     * @param request current request
     * @return bad request response with field level validation details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                fieldErrors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseUtil.error(
                        "Request validation failed", "VALIDATION_ERROR", fieldErrors, request.getRequestURI()));
    }

    /**
     * Handle unparseable request bodies.
     *
     * @param ex      the caught exception
     * @param request current request
     * @return bad request error response
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ResponseUtil.error("Malformed request body", "BAD_REQUEST", null, request.getRequestURI()));
    }

    /**
     * Handle data integrity violations, typically unique constraint violations.
     *
     * @param ex      the caught exception
     * @param request current request
     * @return conflict error response
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ResponseUtil.error(
                        "Data integrity violation", "DATA_INTEGRITY_VIOLATION", null, request.getRequestURI()));
    }

    /**
     * Catch-all handler for any unhandled exception.
     *
     * <p>The internal error message and stack trace are logged but never
     * exposed to the API consumer.</p>
     *
     * @param ex      the caught exception
     * @param request current request
     * @return internal server error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on path {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseUtil.error(
                        "An unexpected error occurred", "INTERNAL_SERVER_ERROR", null, request.getRequestURI()));
    }
}