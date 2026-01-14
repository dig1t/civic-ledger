package com.civicledger.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.Map;

/**
 * Health check endpoints for Kubernetes probes and general health monitoring.
 * Implements /health/ready and /health/live as required by DEVELOPMENT_PLAN.md.
 */
@RestController
public class HealthController {

    @Autowired
    private DataSource dataSource;

    /**
     * Basic health check endpoint.
     */
    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "civic-ledger",
            "timestamp", Instant.now().toString()
        ));
    }

    /**
     * Kubernetes liveness probe.
     * Returns 200 if the application is running.
     * Used to determine if the container should be restarted.
     */
    @GetMapping("/health/live")
    public ResponseEntity<Map<String, Object>> liveness() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "check", "liveness",
            "timestamp", Instant.now().toString()
        ));
    }

    /**
     * Kubernetes readiness probe.
     * Returns 200 if the application is ready to accept traffic.
     * Checks database connectivity before declaring ready.
     */
    @GetMapping("/health/ready")
    public ResponseEntity<Map<String, Object>> readiness() {
        boolean dbHealthy = checkDatabaseHealth();

        if (dbHealthy) {
            return ResponseEntity.ok(Map.of(
                "status", "UP",
                "check", "readiness",
                "database", "UP",
                "timestamp", Instant.now().toString()
            ));
        } else {
            return ResponseEntity.status(503).body(Map.of(
                "status", "DOWN",
                "check", "readiness",
                "database", "DOWN",
                "timestamp", Instant.now().toString()
            ));
        }
    }

    private boolean checkDatabaseHealth() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2); // 2 second timeout
        } catch (Exception e) {
            return false;
        }
    }
}
