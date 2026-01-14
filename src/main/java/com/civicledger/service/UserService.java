package com.civicledger.service;

import com.civicledger.audit.Auditable;
import com.civicledger.entity.AuditLog.ActionType;
import com.civicledger.entity.Role;
import com.civicledger.entity.User;
import com.civicledger.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service for user management operations.
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Find user by ID.
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    /**
     * Find user by email.
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * Find user by Keycloak ID.
     */
    @Transactional(readOnly = true)
    public Optional<User> findByKeycloakId(String keycloakId) {
        return userRepository.findByKeycloakId(keycloakId);
    }

    /**
     * Get all active users.
     */
    @Transactional(readOnly = true)
    public List<User> findAllActive() {
        return userRepository.findByActiveTrue();
    }

    /**
     * Get all users with a specific role.
     */
    @Transactional(readOnly = true)
    public List<User> findByRole(Role role) {
        return userRepository.findByRole(role);
    }

    /**
     * Create a new user with password (for mock auth).
     */
    @Transactional
    @Auditable(action = ActionType.USER_CREATE, resourceType = "USER")
    public User createUser(String email, String password, String firstName,
                           String lastName, Set<Role> roles) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already in use: " + email);
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .keycloakId("local-" + UUID.randomUUID())
                .roles(roles)
                .active(true)
                .build();

        User saved = userRepository.save(user);
        log.info("Created new user: {} with roles: {}", email, roles);
        return saved;
    }

    /**
     * Create or update user from Keycloak JWT claims (JIT provisioning).
     */
    @Transactional
    public User provisionFromKeycloak(String keycloakId, String email,
                                       String firstName, String lastName,
                                       Set<Role> roles) {
        return userRepository.findByKeycloakId(keycloakId)
                .map(existingUser -> {
                    existingUser.setEmail(email);
                    existingUser.setFirstName(firstName);
                    existingUser.setLastName(lastName);
                    existingUser.setRoles(roles);
                    return userRepository.save(existingUser);
                })
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .keycloakId(keycloakId)
                            .email(email)
                            .firstName(firstName)
                            .lastName(lastName)
                            .roles(roles)
                            .active(true)
                            .build();
                    log.info("Provisioned new user from Keycloak: {}", email);
                    return userRepository.save(newUser);
                });
    }

    /**
     * Update user roles.
     */
    @Transactional
    @Auditable(action = ActionType.USER_ROLE_UPDATE, resourceType = "USER", resourceIdExpression = "#userId.toString()")
    public User updateRoles(UUID userId, Set<Role> roles) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setRoles(roles);
        log.info("Updated roles for user {}: {}", user.getEmail(), roles);
        return userRepository.save(user);
    }

    /**
     * Update user clearance level.
     */
    @Transactional
    @Auditable(action = ActionType.USER_CLEARANCE_UPDATE, resourceType = "USER", resourceIdExpression = "#userId.toString()")
    public User updateClearanceLevel(UUID userId, User.ClassificationLevel clearanceLevel) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setClearanceLevel(clearanceLevel);
        log.info("Updated clearance level for user {}: {}", user.getEmail(), clearanceLevel);
        return userRepository.save(user);
    }

    /**
     * Deactivate user account.
     */
    @Transactional
    @Auditable(action = ActionType.USER_DEACTIVATE, resourceType = "USER", resourceIdExpression = "#userId.toString()")
    public void deactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setActive(false);
        userRepository.save(user);
        log.info("Deactivated user: {}", user.getEmail());
    }

    /**
     * Unlock a locked user account.
     */
    @Transactional
    @Auditable(action = ActionType.USER_UNLOCK, resourceType = "USER", resourceIdExpression = "#userId.toString()")
    public void unlockUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);
        log.info("Unlocked user account: {}", user.getEmail());
    }

    /**
     * Check if email is available.
     */
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    /**
     * Check if email is available for a user other than the specified one.
     */
    @Transactional(readOnly = true)
    public boolean isEmailAvailableForUser(String email, UUID userId) {
        return userRepository.findByEmail(email)
                .map(existingUser -> existingUser.getId().equals(userId))
                .orElse(true);
    }

    /**
     * Get all users with pagination.
     */
    @Transactional(readOnly = true)
    public Page<User> findAllPaginated(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Search users by name or email with pagination.
     */
    @Transactional(readOnly = true)
    public Page<User> searchUsers(String search, Pageable pageable) {
        return userRepository.searchUsers(search, pageable);
    }

    /**
     * Update user details.
     */
    @Transactional
    @Auditable(action = ActionType.USER_UPDATE, resourceType = "USER", resourceIdExpression = "#userId.toString()")
    public User updateUser(UUID userId, String email, String firstName, String lastName,
                          String password, Set<Role> roles) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Check if email is available for this user
        if (!isEmailAvailableForUser(email, userId)) {
            throw new IllegalArgumentException("Email already in use: " + email);
        }

        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRoles(roles);

        // Only update password if provided
        if (password != null && !password.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(password));
        }

        log.info("Updated user: {} with roles: {}", email, roles);
        return userRepository.save(user);
    }

    /**
     * Activate user account.
     */
    @Transactional
    @Auditable(action = ActionType.USER_UPDATE, resourceType = "USER", resourceIdExpression = "#userId.toString()")
    public void activateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.setActive(true);
        userRepository.save(user);
        log.info("Activated user: {}", user.getEmail());
    }

    /**
     * Delete user permanently.
     */
    @Transactional
    @Auditable(action = ActionType.USER_DELETE, resourceType = "USER", resourceIdExpression = "#userId.toString()")
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        userRepository.delete(user);
        log.info("Deleted user: {}", user.getEmail());
    }
}
