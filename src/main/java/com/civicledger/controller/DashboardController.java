package com.civicledger.controller;

import com.civicledger.repository.AuditLogRepository;
import com.civicledger.repository.DocumentRepository;
import com.civicledger.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DocumentRepository documentRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public DashboardController(DocumentRepository documentRepository,
                                AuditLogRepository auditLogRepository,
                                UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<DashboardStats> getStats() {
        long totalDocuments = documentRepository.count();

        // Documents this month
        LocalDate now = LocalDate.now();
        LocalDate firstOfMonth = now.with(TemporalAdjusters.firstDayOfMonth());
        Instant startOfMonth = firstOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant();
        long documentsThisMonth = documentRepository.countByCreatedAtAfter(startOfMonth);

        // Audit logs today
        LocalDate today = LocalDate.now();
        Instant startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        long auditLogsToday = auditLogRepository.countByTimestampAfter(startOfDay);

        // Active users
        long activeUsers = userRepository.countByActiveTrue();

        DashboardStats stats = new DashboardStats(
                totalDocuments,
                documentsThisMonth,
                auditLogsToday,
                activeUsers
        );

        return ResponseEntity.ok(stats);
    }

    public record DashboardStats(
            long totalDocuments,
            long documentsThisMonth,
            long auditLogsToday,
            long activeUsers
    ) {}
}
