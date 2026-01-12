package com.civicledger.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * User entity for RBAC support.
 * Primary authentication is handled by Keycloak; this stores supplementary user data.
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email", unique = true),
    @Index(name = "idx_user_keycloak_id", columnList = "keycloakId", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Keycloak subject ID (from JWT 'sub' claim).
     */
    @Column(nullable = false, unique = true)
    private String keycloakId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant lastLoginAt;

    @Column
    private String lastLoginIp;

    /**
     * For account lockout after failed login attempts.
     */
    @Column
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column
    private Instant lockedUntil;

    /**
     * Password hash for mock authentication (dev/demo only).
     * In production, authentication is handled by Keycloak/PIV.
     */
    @Column
    private String passwordHash;

    /**
     * Security clearance level (for document access control).
     */
    @Enumerated(EnumType.STRING)
    @Column
    private ClassificationLevel clearanceLevel;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public boolean isLocked() {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }

    public void recordLoginSuccess(String ipAddress) {
        this.lastLoginAt = Instant.now();
        this.lastLoginIp = ipAddress;
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    public void recordLoginFailure() {
        this.failedLoginAttempts++;
    }

    /**
     * Document classification levels aligned with government standards.
     */
    public enum ClassificationLevel {
        UNCLASSIFIED,
        CUI,           // Controlled Unclassified Information
        CONFIDENTIAL,
        SECRET,
        TOP_SECRET
    }
}
