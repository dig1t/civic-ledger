package com.civicledger.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit log entity for NIST 800-53 AU-2 compliance.
 * Implements Write-Once-Read-Many (WORM) pattern - no updates or deletes allowed.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user_id", columnList = "userId"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_resource_id", columnList = "resourceId"),
    @Index(name = "idx_audit_action_type", columnList = "actionType")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false, length = 45)
    private String ipAddress;

    @Column
    private String resourceId;

    @Column
    private String resourceType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuditStatus status;

    @Column(length = 2000)
    private String details;

    @Column
    private String userAgent;

    @Column
    private String sessionId;

    /**
     * SHA-256 hash of the log entry for tamper detection.
     * Computed from: userId + actionType + timestamp + resourceId + status
     */
    @Column(length = 64)
    private String integrityHash;

    @PrePersist
    protected void onCreate() {
        if (this.timestamp == null) {
            this.timestamp = Instant.now();
        }
    }

    /**
     * Supported audit action types aligned with NIST 800-53 AU-2.
     */
    public enum ActionType {
        // Authentication events
        LOGIN_ATTEMPT,
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        LOGOUT,
        SESSION_EXPIRED,
        TOKEN_REFRESH,

        // Document operations
        DOCUMENT_UPLOAD,
        DOCUMENT_DOWNLOAD,
        DOCUMENT_VIEW,
        DOCUMENT_DELETE,
        DOCUMENT_UPDATE,

        // Administrative actions
        USER_CREATE,
        USER_UPDATE,
        USER_DELETE,
        USER_ROLE_UPDATE,
        USER_CLEARANCE_UPDATE,
        USER_DEACTIVATE,
        USER_UNLOCK,
        ROLE_ASSIGN,
        ROLE_REVOKE,

        // System events
        SYSTEM_START,
        SYSTEM_SHUTDOWN,
        CONFIG_CHANGE,
        SECURITY_ALERT
    }

    public enum AuditStatus {
        SUCCESS,
        FAILURE,
        DENIED,
        ERROR
    }
}
