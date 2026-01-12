package com.civicledger.service;

import com.civicledger.entity.AuditLog;
import com.civicledger.entity.AuditLog.ActionType;
import com.civicledger.entity.AuditLog.AuditStatus;
import com.civicledger.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;

/**
 * Service for managing immutable audit logs.
 * All logs are write-once; updates and deletes are not supported.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Creates an audit log entry. Uses a new transaction to ensure the log is persisted
     * even if the main transaction fails.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog log(ActionType actionType, String resourceType, String resourceId,
                        AuditStatus status, String details, String ipAddress, String userAgent) {
        String userId = getCurrentUserId();
        String sessionId = getCurrentSessionId();

        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .actionType(actionType)
                .timestamp(Instant.now())
                .ipAddress(ipAddress != null ? ipAddress : "unknown")
                .resourceType(resourceType)
                .resourceId(resourceId)
                .status(status)
                .details(truncateDetails(details))
                .userAgent(userAgent)
                .sessionId(sessionId)
                .build();

        // Compute integrity hash
        String integrityHash = computeIntegrityHash(auditLog);
        auditLog = AuditLog.builder()
                .userId(auditLog.getUserId())
                .actionType(auditLog.getActionType())
                .timestamp(auditLog.getTimestamp())
                .ipAddress(auditLog.getIpAddress())
                .resourceType(auditLog.getResourceType())
                .resourceId(auditLog.getResourceId())
                .status(auditLog.getStatus())
                .details(auditLog.getDetails())
                .userAgent(auditLog.getUserAgent())
                .sessionId(auditLog.getSessionId())
                .integrityHash(integrityHash)
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);
        log.debug("Audit log created: {} - {} - {} - {}", actionType, resourceType, resourceId, status);
        return saved;
    }

    /**
     * Async version for non-critical audit events to avoid blocking.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAsync(ActionType actionType, String resourceType, String resourceId,
                         AuditStatus status, String details, String ipAddress, String userAgent) {
        try {
            log(actionType, resourceType, resourceId, status, details, ipAddress, userAgent);
        } catch (Exception e) {
            log.error("Failed to create async audit log: {}", e.getMessage(), e);
        }
    }

    /**
     * Simplified log method for quick audit entries.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog log(ActionType actionType, AuditStatus status, String details) {
        return log(actionType, null, null, status, details, "unknown", null);
    }

    /**
     * Retrieves audit logs with pagination.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    /**
     * Retrieves audit logs for a specific user.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByUser(String userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
    }

    /**
     * Retrieves audit logs for a specific resource.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByResource(String resourceId, Pageable pageable) {
        return auditLogRepository.findByResourceIdOrderByTimestampDesc(resourceId, pageable);
    }

    /**
     * Retrieves audit logs within a time range.
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditLogsByTimeRange(Instant start, Instant end, Pageable pageable) {
        return auditLogRepository.findByTimestampRange(start, end, pageable);
    }

    /**
     * Gets the complete chain of custody for a document.
     */
    @Transactional(readOnly = true)
    public List<AuditLog> getDocumentAuditTrail(String documentId) {
        return auditLogRepository.findDocumentAuditTrail(documentId);
    }

    /**
     * Verifies the integrity of an audit log entry.
     */
    public boolean verifyIntegrity(AuditLog auditLog) {
        if (auditLog.getIntegrityHash() == null) {
            return false;
        }
        String computedHash = computeIntegrityHash(auditLog);
        return auditLog.getIntegrityHash().equals(computedHash);
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return "anonymous";
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof Jwt jwt) {
            // Try common JWT claims for user ID
            String sub = jwt.getClaimAsString("sub");
            if (sub != null) {
                return sub;
            }
            String preferredUsername = jwt.getClaimAsString("preferred_username");
            if (preferredUsername != null) {
                return preferredUsername;
            }
        }

        return auth.getName();
    }

    private String getCurrentSessionId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() instanceof Jwt jwt) {
            return jwt.getClaimAsString("sid");
        }
        return null;
    }

    private String truncateDetails(String details) {
        if (details == null) {
            return null;
        }
        return details.length() > 2000 ? details.substring(0, 1997) + "..." : details;
    }

    private String computeIntegrityHash(AuditLog auditLogEntry) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String data = String.join("|",
                    auditLogEntry.getUserId(),
                    auditLogEntry.getActionType().name(),
                    auditLogEntry.getTimestamp().toString(),
                    auditLogEntry.getResourceId() != null ? auditLogEntry.getResourceId() : "",
                    auditLogEntry.getStatus().name());
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 not available", e);
            return null;
        }
    }
}
