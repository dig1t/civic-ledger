package com.civicledger.controller;

import com.civicledger.dto.ApiError;
import com.civicledger.dto.LoginRequest;
import com.civicledger.dto.LoginResponse;
import com.civicledger.entity.User;
import com.civicledger.exception.AuthenticationException;
import com.civicledger.service.AuthenticationService;
import com.civicledger.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller for mock/dev authentication.
 * This endpoint is for portfolio demonstration purposes.
 * Production authentication uses Keycloak with PIV/CAC cards.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationService authenticationService;
    private final UserService userService;

    public AuthController(AuthenticationService authenticationService, UserService userService) {
        this.authenticationService = authenticationService;
        this.userService = userService;
    }

    /**
     * Mock login endpoint for development/demo.
     * Accepts email/password credentials and returns a JWT token.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = getClientIpAddress(httpRequest);
        log.info("Login request received for email: {} from IP: {}", request.getEmail(), ipAddress);

        LoginResponse response = authenticationService.authenticate(request, ipAddress);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current authenticated user's information.
     */
    @GetMapping("/me")
    public ResponseEntity<LoginResponse.UserDTO> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        LoginResponse.UserDTO userDTO = LoginResponse.UserDTO.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(user.getRoles().stream()
                        .map(role -> role.name())
                        .collect(java.util.stream.Collectors.toSet()))
                .clearanceLevel(user.getClearanceLevel() != null
                        ? user.getClearanceLevel().name()
                        : null)
                .build();

        return ResponseEntity.ok(userDTO);
    }

    /**
     * Token refresh endpoint (placeholder for future implementation).
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiError> refresh() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(ApiError.of(501, "Not Implemented",
                        "Token refresh not yet implemented", "/api/auth/refresh"));
    }

    /**
     * Logout endpoint - client should discard the token.
     * Server-side token invalidation would require a token blacklist.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // In a stateless JWT system, logout is handled client-side.
        // For production, implement a token blacklist or short-lived tokens.
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {

        log.warn("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(401, "Unauthorized",
                        ex.getMessage(), request.getRequestURI()));
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
