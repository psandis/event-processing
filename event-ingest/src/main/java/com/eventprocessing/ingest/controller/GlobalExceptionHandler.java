package com.eventprocessing.ingest.controller;

import com.eventprocessing.ingest.service.BatchSizeExceededException;
import com.eventprocessing.ingest.service.EventSubmissionException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<Map<String, String>> errors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.add(error(error.getField(), error.getDefaultMessage()));
        }
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation Error", "Validation failed", request);
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<Map<String, String>> errors = ex.getConstraintViolations().stream()
                .map(this::constraintViolationError)
                .toList();
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation Error", "Validation failed", request);
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(BatchSizeExceededException.class)
    public ProblemDetail handleBatchTooLarge(BatchSizeExceededException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Batch Size Exceeded", ex.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Malformed Request", "Request body could not be parsed", request);
    }

    @ExceptionHandler(EventSubmissionException.class)
    public ProblemDetail handleSubmissionFailure(EventSubmissionException ex, HttpServletRequest request) {
        log.error("Event submission failed", ex);
        return problem(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Submission Failed",
                "Event could not be submitted at this time",
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error while handling request", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Error", "An unexpected error occurred", request);
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://httpstatuses.com/" + status.value()));
        problem.setProperty("timestamp", Instant.now().toString());
        problem.setProperty("path", request.getRequestURI());
        return problem;
    }

    private Map<String, String> constraintViolationError(ConstraintViolation<?> violation) {
        String field = violation.getPropertyPath().toString();
        int separatorIndex = field.lastIndexOf('.');
        if (separatorIndex >= 0) {
            field = field.substring(separatorIndex + 1);
        }
        return error(field, violation.getMessage());
    }

    private Map<String, String> error(String field, String message) {
        Map<String, String> error = new LinkedHashMap<>();
        error.put("field", field);
        error.put("message", message);
        return error;
    }
}
