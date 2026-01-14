package com.civicledger.dto;

import com.civicledger.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DTO for user data in API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private String id;
    private String email;
    private String fullName;
    private Set<String> roles;
    private boolean enabled;
    private String createdAt;
    private String lastLoginAt;
    private String clearanceLevel;

    /**
     * Convert User entity to DTO.
     */
    public static UserDTO fromEntity(User user) {
        return UserDTO.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(user.getRoles().stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet()))
                .enabled(user.isActive())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .lastLoginAt(user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null)
                .clearanceLevel(user.getClearanceLevel() != null ? user.getClearanceLevel().name() : null)
                .build();
    }
}
