package com.urlshortener.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all controllers.
 * Converts exceptions into proper HTTP responses with error messages.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle validation errors (from @Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ErrorResponse response = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            errors
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle UrlNotFoundException (404 Not Found)
     */
    @ExceptionHandler(UrlNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUrlNotFoundException(UrlNotFoundException ex) {
        ErrorResponse response = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle RuntimeException (generic errors from services)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex) {
        // Check for specific error messages
        if (ex.getMessage().contains("already taken") || ex.getMessage().contains("already exists")) {
            return handleConflict(ex);
        } else if (ex.getMessage().contains("not authorized") || ex.getMessage().contains("unauthorized")) {
            return handleUnauthorized(ex);
        }
        
        // Generic error
        ErrorResponse response = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            null
        );
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Handle 409 Conflict errors (duplicate resources)
     */
    private ResponseEntity<ErrorResponse> handleConflict(RuntimeException ex) {
        ErrorResponse response = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            ex.getMessage(),
            null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * Handle 401 Unauthorized errors
     */
    private ResponseEntity<ErrorResponse> handleUnauthorized(RuntimeException ex) {
        ErrorResponse response = new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            ex.getMessage(),
            null
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * Handle all other unhandled exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse response = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred",
            null
        );
        
        // Log the exception for debugging
        ex.printStackTrace();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Error response structure
     */
    public static class ErrorResponse {
        private int status;
        private String message;
        private LocalDateTime timestamp;
        private Map<String, String> errors;

        public ErrorResponse(int status, String message, Map<String, String> errors) {
            this.status = status;
            this.message = message;
            this.timestamp = LocalDateTime.now();
            this.errors = errors;
        }

        // Getters
        public int getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }

        public Map<String, String> getErrors() {
            return errors;
        }
    }
}