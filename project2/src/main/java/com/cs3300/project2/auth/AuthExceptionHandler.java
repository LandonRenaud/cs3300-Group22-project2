package com.cs3300.project2.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(AuthExceptionHandler.class);

    @ExceptionHandler(FirebaseAuthException.class)
    public ResponseEntity<Map<String, Object>> firebaseRejected(FirebaseAuthException exception) {
        log.warn("Firebase Authentication rejected request: HTTP {}, code='{}', message='{}'",
                exception.firebaseStatus(), exception.firebaseCode(), exception.getMessage());
        return ResponseEntity.badRequest().body(Map.of(
                "detail", exception.getMessage(),
                "firebaseCode", exception.firebaseCode(),
                "firebaseStatus", exception.firebaseStatus()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badCredentials(IllegalArgumentException exception) {
        log.warn("Authentication request rejected: {}", exception.getMessage());
        return ResponseEntity.badRequest().body(Map.of("detail", exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> firebaseUnavailable(IllegalStateException exception) {
        log.error("Authentication service error: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("detail", exception.getMessage() == null
                        ? "Firebase Authentication is unavailable." : exception.getMessage()));
    }
}
