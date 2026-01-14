package com.civicledger.controller;

import com.civicledger.entity.AuditLog;
import com.civicledger.entity.AuditLog.ActionType;
import com.civicledger.entity.AuditLog.AuditStatus;
import com.civicledger.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        createTestAuditLogs();
    }

    private void createTestAuditLogs() {
        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        Instant lastWeek = now.minus(7, ChronoUnit.DAYS);

        // Login success - user1 - today
        auditLogRepository.save(AuditLog.builder()
                .userId("user1")
                .actionType(ActionType.LOGIN_SUCCESS)
                .timestamp(now)
                .ipAddress("192.168.1.1")
                .status(AuditStatus.SUCCESS)
                .details("User logged in successfully")
                .build());

        // Login failure - user1 - yesterday
        auditLogRepository.save(AuditLog.builder()
                .userId("user1")
                .actionType(ActionType.LOGIN_FAILURE)
                .timestamp(yesterday)
                .ipAddress("192.168.1.1")
                .status(AuditStatus.FAILURE)
                .details("Invalid password")
                .build());

        // Document upload - user2 - today
        auditLogRepository.save(AuditLog.builder()
                .userId("user2")
                .actionType(ActionType.DOCUMENT_UPLOAD)
                .timestamp(now)
                .ipAddress("192.168.1.2")
                .resourceType("DOCUMENT")
                .resourceId("doc-123")
                .status(AuditStatus.SUCCESS)
                .details("Uploaded test.pdf")
                .build());

        // Document download - user2 - last week
        auditLogRepository.save(AuditLog.builder()
                .userId("user2")
                .actionType(ActionType.DOCUMENT_DOWNLOAD)
                .timestamp(lastWeek)
                .ipAddress("192.168.1.2")
                .resourceType("DOCUMENT")
                .resourceId("doc-456")
                .status(AuditStatus.SUCCESS)
                .details("Downloaded report.pdf")
                .build());

        // User create - admin - today
        auditLogRepository.save(AuditLog.builder()
                .userId("admin")
                .actionType(ActionType.USER_CREATE)
                .timestamp(now)
                .ipAddress("192.168.1.100")
                .resourceType("USER")
                .resourceId("new-user-id")
                .status(AuditStatus.SUCCESS)
                .details("Created new user account")
                .build());
    }

    @Nested
    @DisplayName("GET /api/audit-logs - No Filters")
    class NoFiltersTests {

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should return all audit logs when no filters applied")
        void shouldReturnAllLogs() throws Exception {
            mockMvc.perform(get("/api/audit-logs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(5)))
                    .andExpect(jsonPath("$.totalElements", is(5)))
                    .andExpect(jsonPath("$.content[*].actionType", containsInAnyOrder(
                            "LOGIN_SUCCESS", "LOGIN_FAILURE", "DOCUMENT_UPLOAD",
                            "DOCUMENT_DOWNLOAD", "USER_CREATE")));
        }

        @Test
        @WithMockUser(roles = "AUDITOR")
        @DisplayName("Should allow AUDITOR role to access audit logs")
        void auditorShouldAccess() throws Exception {
            mockMvc.perform(get("/api/audit-logs"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "OFFICER")
        @DisplayName("Should deny OFFICER role access to audit logs")
        void officerShouldBeDenied() throws Exception {
            mockMvc.perform(get("/api/audit-logs"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("GET /api/audit-logs - Action Type Filter")
    class ActionTypeFilterTests {

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should filter by LOGIN_SUCCESS action type")
        void shouldFilterByLoginSuccess() throws Exception {
            mockMvc.perform(get("/api/audit-logs")
                            .param("actionType", "LOGIN_SUCCESS"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].actionType", is("LOGIN_SUCCESS")))
                    .andExpect(jsonPath("$.content[0].userId", is("user1")));
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should filter by DOCUMENT_UPLOAD action type")
        void shouldFilterByDocumentUpload() throws Exception {
            mockMvc.perform(get("/api/audit-logs")
                            .param("actionType", "DOCUMENT_UPLOAD"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].actionType", is("DOCUMENT_UPLOAD")))
                    .andExpect(jsonPath("$.content[0].resourceId", is("doc-123")));
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should return empty array for non-existent action type filter")
        void shouldReturnEmptyForNonExistentActionType() throws Exception {
            mockMvc.perform(get("/api/audit-logs")
                            .param("actionType", "LOGOUT"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should ignore invalid action type gracefully")
        void shouldIgnoreInvalidActionType() throws Exception {
            mockMvc.perform(get("/api/audit-logs")
                            .param("actionType", "INVALID_TYPE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(5))); // Returns all logs
        }
    }

    @Nested
    @DisplayName("GET /api/audit-logs - User ID Filter")
    class UserIdFilterTests {

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should filter by user ID")
        void shouldFilterByUserId() throws Exception {
            mockMvc.perform(get("/api/audit-logs")
                            .param("userId", "user1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[*].userId", everyItem(is("user1"))));
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should filter by different user ID")
        void shouldFilterByUser2() throws Exception {
            mockMvc.perform(get("/api/audit-logs")
                            .param("userId", "user2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[*].userId", everyItem(is("user2"))));
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should return empty for non-existent user ID")
        void shouldReturnEmptyForNonExistentUser() throws Exception {
            mockMvc.perform(get("/api/audit-logs")
                            .param("userId", "nonexistent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/audit-logs - Date Range Filter")
    class DateRangeFilterTests {

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should filter by date range - today only")
        void shouldFilterByToday() throws Exception {
            String today = LocalDate.now().toString();

            mockMvc.perform(get("/api/audit-logs")
                            .param("startDate", today)
                            .param("endDate", today))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(3))); // 3 logs created today
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should filter by date range - last 7 days")
        void shouldFilterByLastWeek() throws Exception {
            String today = LocalDate.now().toString();
            String lastWeek = LocalDate.now().minusDays(7).toString();

            mockMvc.perform(get("/api/audit-logs")
                            .param("startDate", lastWeek)
                            .param("endDate", today))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(5))); // All logs
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should return empty for future date range")
        void shouldReturnEmptyForFutureDates() throws Exception {
            String tomorrow = LocalDate.now().plusDays(1).toString();
            String nextWeek = LocalDate.now().plusDays(7).toString();

            mockMvc.perform(get("/api/audit-logs")
                            .param("startDate", tomorrow)
                            .param("endDate", nextWeek))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/audit-logs - Combined Filters")
    class CombinedFiltersTests {

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should combine actionType and userId filters")
        void shouldCombineActionTypeAndUserId() throws Exception {
            mockMvc.perform(get("/api/audit-logs")
                            .param("actionType", "LOGIN_SUCCESS")
                            .param("userId", "user1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].actionType", is("LOGIN_SUCCESS")))
                    .andExpect(jsonPath("$.content[0].userId", is("user1")));
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should combine all filters")
        void shouldCombineAllFilters() throws Exception {
            String today = LocalDate.now().toString();

            mockMvc.perform(get("/api/audit-logs")
                            .param("actionType", "DOCUMENT_UPLOAD")
                            .param("userId", "user2")
                            .param("startDate", today)
                            .param("endDate", today))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].actionType", is("DOCUMENT_UPLOAD")))
                    .andExpect(jsonPath("$.content[0].userId", is("user2")));
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should return empty when combined filters have no match")
        void shouldReturnEmptyWhenNoMatch() throws Exception {
            mockMvc.perform(get("/api/audit-logs")
                            .param("actionType", "DOCUMENT_UPLOAD")
                            .param("userId", "user1")) // user1 has no document uploads
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/audit-logs - Pagination")
    class PaginationTests {

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should respect page size parameter")
        void shouldRespectPageSize() throws Exception {
            mockMvc.perform(get("/api/audit-logs")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.size", is(2)))
                    .andExpect(jsonPath("$.totalElements", is(5)))
                    .andExpect(jsonPath("$.totalPages", is(3)));
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should return correct page")
        void shouldReturnCorrectPage() throws Exception {
            // Page 0 with size 2
            mockMvc.perform(get("/api/audit-logs")
                            .param("page", "0")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.page", is(0)))
                    .andExpect(jsonPath("$.first", is(true)));

            // Page 1 with size 2
            mockMvc.perform(get("/api/audit-logs")
                            .param("page", "1")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.page", is(1)));

            // Page 2 with size 2 (should have 1 item)
            mockMvc.perform(get("/api/audit-logs")
                            .param("page", "2")
                            .param("size", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.page", is(2)))
                    .andExpect(jsonPath("$.last", is(true)));
        }
    }
}
