package com.civicledger.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

/**
 * Global exception handler for REST API error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InsufficientClearanceException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientClearance(InsufficientClearanceException ex) {
        log.warn("Access denied due to insufficient clearance: {}", ex.getMessage());

        // Return generic error message to avoid revealing document existence
        Map<String, Object> response = Map.of(
                "error", "ACCESS_DENIED",
                "message", "Insufficient clearance level to access this resource",
                "timestamp", Instant.now().toString()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());

        Map<String, Object> response = Map.of(
                "error", "AUTHENTICATION_FAILED",
                "message", ex.getMessage(),
                "timestamp", Instant.now().toString()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<Map<String, Object>> handleStorageException(StorageException ex) {
        log.error("Storage error: {}", ex.getMessage());

        Map<String, Object> response = Map.of(
                "error", "STORAGE_ERROR",
                "message", "An error occurred while processing the file",
                "timestamp", Instant.now().toString()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    @ExceptionHandler(CryptoException.class)
    public ResponseEntity<Map<String, Object>> handleCryptoException(CryptoException ex) {
        log.error("Cryptographic error: {}", ex.getMessage());

        Map<String, Object> response = Map.of(
                "error", "CRYPTO_ERROR",
                "message", "An error occurred while processing encryption",
                "timestamp", Instant.now().toString()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
