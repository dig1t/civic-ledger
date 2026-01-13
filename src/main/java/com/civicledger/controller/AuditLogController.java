package com.civicledger.controller;

import com.civicledger.entity.AuditLog;
import com.civicledger.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public ResponseEntity<List<AuditLogDTO>> listAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("timestamp").descending());

        // Build dynamic specification combining all filters
        Specification<AuditLog> spec = buildSpecification(actionType, userId, startDate, endDate);

        Page<AuditLog> logs = auditLogRepository.findAll(spec, pageRequest);
        List<AuditLogDTO> dtos = logs.map(this::toDTO).getContent();
        return ResponseEntity.ok(dtos);
    }

    private Specification<AuditLog> buildSpecification(String actionType, String userId,
                                                        String startDate, String endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by action type
            if (actionType != null && !actionType.isBlank()) {
                try {
                    AuditLog.ActionType type = AuditLog.ActionType.valueOf(actionType);
                    predicates.add(cb.equal(root.get("actionType"), type));
                } catch (IllegalArgumentException e) {
                    // Invalid action type - ignore filter
                }
            }

            // Filter by user ID
            if (userId != null && !userId.isBlank()) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }

            // Filter by date range
            if (startDate != null && !startDate.isBlank()) {
                try {
                    Instant start = LocalDate.parse(startDate)
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant();
                    predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), start));
                } catch (Exception e) {
                    // Invalid date - ignore filter
                }
            }

            if (endDate != null && !endDate.isBlank()) {
                try {
                    Instant end = LocalDate.parse(endDate)
                            .plusDays(1)
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant();
                    predicates.add(cb.lessThan(root.get("timestamp"), end));
                } catch (Exception e) {
                    // Invalid date - ignore filter
                }
            }

            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    @GetMapping("/{resourceId}/trail")
    public ResponseEntity<?> getDocumentAuditTrail(@PathVariable String resourceId) {
        var trail = auditLogRepository.findDocumentAuditTrail(resourceId);
        return ResponseEntity.ok(trail.stream().map(this::toDTO).toList());
    }

    private AuditLogDTO toDTO(AuditLog log) {
        return new AuditLogDTO(
                log.getId().toString(),
                log.getUserId(),
                log.getActionType().name(),
                log.getTimestamp().toString(),
                log.getIpAddress(),
                log.getResourceId(),
                log.getResourceType(),
                log.getStatus().name(),
                log.getDetails()
        );
    }

    public record AuditLogDTO(
            String id,
            String userId,
            String actionType,
            String timestamp,
            String ipAddress,
            String resourceId,
            String resourceType,
            String status,
            String details
    ) {}
}
