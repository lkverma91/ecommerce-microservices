package com.ecommerce.paymentservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice // Global exception handler for the Payment Service
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)// Handle ResourceNotFoundException and return a 404 response
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildError(HttpStatus.NOT_FOUND, ex.getMessage(), req));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(buildError(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), req));
    }

    private ErrorResponse buildError(HttpStatus status, String message, HttpServletRequest req) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(req != null ? req.getRequestURI() : null)
                .traceId(MDC.get("traceId")) //what is mdc https://www.baeldung.com/spring-mvc-exceptionhandler-best-practices#1-use-mdc-to-log-trace-ids
                .build();
    }
}
