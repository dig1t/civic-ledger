package com.civicledger.controller;

import com.civicledger.dto.*;
import com.civicledger.entity.Role;
import com.civicledger.entity.User;
import com.civicledger.repository.UserRepository;
import com.civicledger.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * REST controller for user management operations.
 * Only ADMINISTRATOR role can create, update, or delete users.
 * All authenticated users can view the user list.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final UserRepository userRepository;

    public UserController(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    /**
     * List all users with pagination, optional search, and filters.
     * Accessible by all authenticated users.
     */
    @GetMapping
    public ResponseEntity<PagedResponse<UserDTO>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String clearanceLevel) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Parse role filter if provided
        Role roleFilter = null;
        if (role != null && !role.isBlank()) {
            try {
                roleFilter = Role.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.debug("Invalid role filter: {}", role);
            }
        }

        // Parse clearance level filter if provided
        User.ClassificationLevel clearanceFilter = null;
        if (clearanceLevel != null && !clearanceLevel.isBlank()) {
            try {
                clearanceFilter = User.ClassificationLevel.valueOf(clearanceLevel.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.debug("Invalid clearance level filter: {}", clearanceLevel);
            }
        }

        Page<User> users = findUsersWithFilters(search, roleFilter, clearanceFilter, pageRequest);

        PagedResponse<UserDTO> response = PagedResponse.fromPage(users, UserDTO::fromEntity);
        return ResponseEntity.ok(response);
    }

    /**
     * Find users with optional search and filters.
     */
    private Page<User> findUsersWithFilters(String search, Role role,
            User.ClassificationLevel clearanceLevel, PageRequest pageRequest) {
        boolean hasSearch = search != null && !search.isBlank();
        boolean hasRole = role != null;
        boolean hasClearance = clearanceLevel != null;

        if (hasSearch && hasRole && hasClearance) {
            return userRepository.searchUsersByRoleAndClearanceLevel(search, role, clearanceLevel, pageRequest);
        } else if (hasSearch && hasRole) {
            return userRepository.searchUsersByRole(search, role, pageRequest);
        } else if (hasSearch && hasClearance) {
            return userRepository.searchUsersByClearanceLevel(search, clearanceLevel, pageRequest);
        } else if (hasSearch) {
            return userService.searchUsers(search, pageRequest);
        } else if (hasRole && hasClearance) {
            return userRepository.findByRoleAndClearanceLevel(role, clearanceLevel, pageRequest);
        } else if (hasRole) {
            return userRepository.findByRolePaginated(role, pageRequest);
        } else if (hasClearance) {
            return userRepository.findByClearanceLevel(clearanceLevel, pageRequest);
        } else {
            return userService.findAllPaginated(pageRequest);
        }
    }

    /**
     * Get a single user by ID.
     * Accessible by all authenticated users.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable UUID id) {
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(UserDTO.fromEntity(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new user.
     * Only accessible by ADMINISTRATOR role.
     */
    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            // Parse role
            Role role;
            try {
                role = Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(ApiError.of(HttpStatus.BAD_REQUEST.value(), "Bad Request",
                                "Invalid role: " + request.getRole(), "/api/users"));
            }

            // Parse full name into first and last name
            String[] nameParts = parseFullName(request.getFullName());

            User user = userService.createUser(
                    request.getEmail(),
                    request.getPassword(),
                    nameParts[0],
                    nameParts[1],
                    new HashSet<>(Set.of(role))
            );

            log.info("Created new user: {}", user.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(UserDTO.fromEntity(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiError.of(HttpStatus.BAD_REQUEST.value(), "Bad Request",
                            e.getMessage(), "/api/users"));
        }
    }

    /**
     * Update an existing user.
     * Only accessible by ADMINISTRATOR role.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request) {
        try {
            // Parse role
            Role role;
            try {
                role = Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(ApiError.of(HttpStatus.BAD_REQUEST.value(), "Bad Request",
                                "Invalid role: " + request.getRole(), "/api/users/" + id));
            }

            // Parse full name into first and last name
            String[] nameParts = parseFullName(request.getFullName());

            User user = userService.updateUser(
                    id,
                    request.getEmail(),
                    nameParts[0],
                    nameParts[1],
                    request.getPassword(),
                    new HashSet<>(Set.of(role))
            );

            log.info("Updated user: {}", user.getEmail());
            return ResponseEntity.ok(UserDTO.fromEntity(user));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest()
                    .body(ApiError.of(HttpStatus.BAD_REQUEST.value(), "Bad Request",
                            e.getMessage(), "/api/users/" + id));
        }
    }

    /**
     * Update user status (enable/disable).
     * Only accessible by ADMINISTRATOR role.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        try {
            if (request.getEnabled()) {
                userService.activateUser(id);
            } else {
                userService.deactivateUser(id);
            }

            return userService.findById(id)
                    .map(user -> ResponseEntity.ok(UserDTO.fromEntity(user)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest()
                    .body(ApiError.of(HttpStatus.BAD_REQUEST.value(), "Bad Request",
                            e.getMessage(), "/api/users/" + id + "/status"));
        }
    }

    /**
     * Delete a user.
     * Only accessible by ADMINISTRATOR role.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable UUID id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest()
                    .body(ApiError.of(HttpStatus.BAD_REQUEST.value(), "Bad Request",
                            e.getMessage(), "/api/users/" + id));
        }
    }

    /**
     * Parse full name into first name and last name.
     * If only one name is provided, use it as first name and set last name to empty.
     */
    private String[] parseFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new String[]{"", ""};
        }

        String trimmed = fullName.trim();
        int lastSpace = trimmed.lastIndexOf(' ');

        if (lastSpace == -1) {
            return new String[]{trimmed, ""};
        }

        return new String[]{
                trimmed.substring(0, lastSpace).trim(),
                trimmed.substring(lastSpace + 1).trim()
        };
    }
}
