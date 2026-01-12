package com.civicledger.service;

import com.civicledger.audit.Auditable;
import com.civicledger.dto.LoginRequest;
import com.civicledger.dto.LoginResponse;
import com.civicledger.entity.AuditLog.ActionType;
import com.civicledger.entity.Role;
import com.civicledger.entity.User;
import com.civicledger.exception.AuthenticationException;
import com.civicledger.repository.UserRepository;
import com.civicledger.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Service handling authentication for mock/dev authentication.
 * In production, authentication is delegated to Keycloak with PIV/CAC support.
 */
@Service
public class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Value("${civicledger.auth.mock-mfa-code:123456}")
    private String mockMfaCode;

    @Value("${civicledger.auth.require-mfa:false}")
    private boolean requireMfa;

    public AuthenticationService(UserRepository userRepository,
                                  JwtService jwtService,
                                  PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticate user with email and password (mock auth for dev/demo).
     */
    @Transactional
    @Auditable(action = ActionType.LOGIN_ATTEMPT, resourceType = "USER")
    public LoginResponse authenticate(LoginRequest request, String ipAddress) {
        log.debug("Authentication attempt for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found for email {}", request.getEmail());
                    return new AuthenticationException("Invalid credentials");
                });

        // Check if account is locked
        if (user.isLocked()) {
            log.warn("Login failed: account locked for user {}", user.getEmail());
            throw new AuthenticationException("Account is locked. Please try again later.");
        }

        // Check if account is active
        if (!user.isActive()) {
            log.warn("Login failed: account inactive for user {}", user.getEmail());
            throw new AuthenticationException("Account is disabled");
        }

        // Validate password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new AuthenticationException("Invalid credentials");
        }

        // Validate MFA if required
        if (requireMfa) {
            if (request.getMfaCode() == null || !request.getMfaCode().equals(mockMfaCode)) {
                log.warn("Login failed: invalid MFA code for user {}", user.getEmail());
                throw new AuthenticationException("Invalid MFA code");
            }
        }

        // Successful login
        user.recordLoginSuccess(ipAddress);
        userRepository.save(user);

        String token = jwtService.generateToken(user);

        log.info("User {} logged in successfully from IP {}", user.getEmail(), ipAddress);

        return LoginResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationMinutes() * 60)
                .user(mapToUserDTO(user))
                .build();
    }

    private void handleFailedLogin(User user) {
        user.recordLoginFailure();

        if (user.getFailedLoginAttempts() >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plus(LOCKOUT_DURATION));
            log.warn("Account locked for user {} due to {} failed attempts",
                    user.getEmail(), user.getFailedLoginAttempts());
        }

        userRepository.save(user);
    }

    private LoginResponse.UserDTO mapToUserDTO(User user) {
        return LoginResponse.UserDTO.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(user.getRoles().stream()
                        .map(Role::name)
                        .collect(Collectors.toSet()))
                .clearanceLevel(user.getClearanceLevel() != null
                        ? user.getClearanceLevel().name()
                        : null)
                .build();
    }
}
