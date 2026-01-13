package com.civicledger.repository;

import com.civicledger.entity.AuditLog;
import com.civicledger.entity.AuditLog.ActionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for immutable audit logs.
 * Note: Only insert operations should be used. Updates and deletes violate WORM compliance.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    /**
     * Find all audit logs for a specific user.
     */
    Page<AuditLog> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    /**
     * Find all audit logs for a specific resource.
     */
    Page<AuditLog> findByResourceIdOrderByTimestampDesc(String resourceId, Pageable pageable);

    /**
     * Find audit logs by action type.
     */
    Page<AuditLog> findByActionTypeOrderByTimestampDesc(ActionType actionType, Pageable pageable);

    /**
     * Find audit logs within a time range.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :start AND :end ORDER BY a.timestamp DESC")
    Page<AuditLog> findByTimestampRange(
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable);

    /**
     * Find audit logs by user and time range.
     */
    @Query("SELECT a FROM AuditLog a WHERE a.userId = :userId AND a.timestamp BETWEEN :start AND :end ORDER BY a.timestamp DESC")
    Page<AuditLog> findByUserIdAndTimestampRange(
            @Param("userId") String userId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable);

    /**
     * Find security-related events (login failures, denied access, security alerts).
     */
    @Query("SELECT a FROM AuditLog a WHERE a.actionType IN :actionTypes AND a.timestamp >= :since ORDER BY a.timestamp DESC")
    List<AuditLog> findSecurityEvents(
            @Param("actionTypes") List<ActionType> actionTypes,
            @Param("since") Instant since);

    /**
     * Count failed login attempts for a user within a time window (for lockout detection).
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.userId = :userId AND a.actionType = 'LOGIN_FAILURE' AND a.timestamp >= :since")
    long countFailedLoginAttempts(@Param("userId") String userId, @Param("since") Instant since);

    /**
     * Get the complete audit trail for a document (chain of custody).
     */
    @Query("SELECT a FROM AuditLog a WHERE a.resourceType = 'DOCUMENT' AND a.resourceId = :documentId ORDER BY a.timestamp ASC")
    List<AuditLog> findDocumentAuditTrail(@Param("documentId") String documentId);

    /**
     * Count audit logs after a given timestamp.
     */
    long countByTimestampAfter(Instant since);
}
