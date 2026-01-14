package com.civicledger.controller;

import com.civicledger.entity.Role;
import com.civicledger.entity.User;
import com.civicledger.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private User adminUser;
    private User officerUser;
    private User auditorUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        createTestUsers();
    }

    private void createTestUsers() {
        // Admin user
        adminUser = userRepository.save(User.builder()
                .email("admin@example.com")
                .firstName("Admin")
                .lastName("User")
                .keycloakId("keycloak-admin-" + UUID.randomUUID())
                .passwordHash(passwordEncoder.encode("adminpassword123"))
                .roles(new HashSet<>(Set.of(Role.ADMINISTRATOR)))
                .active(true)
                .createdAt(Instant.now())
                .build());

        // Officer user
        officerUser = userRepository.save(User.builder()
                .email("officer@example.com")
                .firstName("Officer")
                .lastName("User")
                .keycloakId("keycloak-officer-" + UUID.randomUUID())
                .passwordHash(passwordEncoder.encode("officerpassword123"))
                .roles(new HashSet<>(Set.of(Role.OFFICER)))
                .active(true)
                .createdAt(Instant.now())
                .build());

        // Auditor user
        auditorUser = userRepository.save(User.builder()
                .email("auditor@example.com")
                .firstName("Auditor")
                .lastName("User")
                .keycloakId("keycloak-auditor-" + UUID.randomUUID())
                .passwordHash(passwordEncoder.encode("auditorpassword123"))
                .roles(new HashSet<>(Set.of(Role.AUDITOR)))
                .active(true)
                .createdAt(Instant.now())
                .build());

        // Additional users for pagination tests
        for (int i = 1; i <= 5; i++) {
            userRepository.save(User.builder()
                    .email("user" + i + "@example.com")
                    .firstName("Test")
                    .lastName("User" + i)
                    .keycloakId("keycloak-user-" + UUID.randomUUID())
                    .passwordHash(passwordEncoder.encode("password12345678"))
                    .roles(new HashSet<>(Set.of(Role.OFFICER)))
                    .active(i % 2 == 0) // Some active, some inactive
                    .createdAt(Instant.now().minusSeconds(i * 86400)) // Different creation dates
                    .build());
        }
    }

    @Nested
    @DisplayName("GET /api/users - List Users")
    class ListUsersTests {

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Administrator should see all users")
        void adminShouldSeeAllUsers() throws Exception {
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(8)))
                    .andExpect(jsonPath("$.totalElements", is(8)))
                    .andExpect(jsonPath("$.page", is(0)));
        }

        @Test
        @WithMockUser(roles = "OFFICER")
        @DisplayName("Officer should see all users")
        void officerShouldSeeAllUsers() throws Exception {
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(8)));
        }

        @Test
        @WithMockUser(roles = "AUDITOR")
        @DisplayName("Auditor should see all users")
        void auditorShouldSeeAllUsers() throws Exception {
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(8)));
        }

        @Test
        @DisplayName("Unauthenticated request should be forbidden")
        void unauthenticatedShouldBeForbidden() throws Exception {
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should respect page size parameter")
        void shouldRespectPageSize() throws Exception {
            mockMvc.perform(get("/api/users")
                            .param("size", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(3)))
                    .andExpect(jsonPath("$.size", is(3)))
                    .andExpect(jsonPath("$.totalPages", greaterThan(1)));
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should return correct page")
        void shouldReturnCorrectPage() throws Exception {
            mockMvc.perform(get("/api/users")
                            .param("page", "1")
                            .param("size", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page", is(1)))
                    .andExpect(jsonPath("$.first", is(false)));
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should search by name")
        void shouldSearchByName() throws Exception {
            mockMvc.perform(get("/api/users")
                            .param("search", "Admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].fullName", is("Admin User")));
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should search by email")
        void shouldSearchByEmail() throws Exception {
            mockMvc.perform(get("/api/users")
                            .param("search", "officer@"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].email", is("officer@example.com")));
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should return empty for non-matching search")
        void shouldReturnEmptyForNoMatch() throws Exception {
            mockMvc.perform(get("/api/users")
                            .param("search", "nonexistent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/users/{id} - Get Single User")
    class GetUserTests {

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should return user by ID")
        void shouldReturnUserById() throws Exception {
            mockMvc.perform(get("/api/users/" + adminUser.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id", is(adminUser.getId().toString())))
                    .andExpect(jsonPath("$.email", is("admin@example.com")))
                    .andExpect(jsonPath("$.fullName", is("Admin User")))
                    .andExpect(jsonPath("$.roles", contains("ADMINISTRATOR")))
                    .andExpect(jsonPath("$.enabled", is(true)));
        }

        @Test
        @WithMockUser(roles = "OFFICER")
        @DisplayName("Officer should access user details")
        void officerShouldAccessUserDetails() throws Exception {
            mockMvc.perform(get("/api/users/" + adminUser.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email", is("admin@example.com")));
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should return 404 for non-existent user")
        void shouldReturn404ForNonExistent() throws Exception {
            mockMvc.perform(get("/api/users/" + UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/users - Create User")
    class CreateUserTests {

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Administrator should create user")
        void adminShouldCreateUser() throws Exception {
            Map<String, String> request = Map.of(
                    "email", "newuser@example.com",
                    "fullName", "New User",
                    "password", "securepassword123",
                    "role", "OFFICER"
            );

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email", is("newuser@example.com")))
                    .andExpect(jsonPath("$.fullName", is("New User")))
                    .andExpect(jsonPath("$.roles", contains("OFFICER")))
                    .andExpect(jsonPath("$.enabled", is(true)));
        }

        @Test
        @WithMockUser(roles = "OFFICER")
        @DisplayName("Officer should be forbidden to create user")
        void officerShouldBeForbidden() throws Exception {
            Map<String, String> request = Map.of(
                    "email", "newuser@example.com",
                    "fullName", "New User",
                    "password", "securepassword123",
                    "role", "OFFICER"
            );

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "AUDITOR")
        @DisplayName("Auditor should be forbidden to create user")
        void auditorShouldBeForbidden() throws Exception {
            Map<String, String> request = Map.of(
                    "email", "newuser@example.com",
                    "fullName", "New User",
                    "password", "securepassword123",
                    "role", "OFFICER"
            );

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should reject duplicate email")
        void shouldRejectDuplicateEmail() throws Exception {
            Map<String, String> request = Map.of(
                    "email", "admin@example.com", // Already exists
                    "fullName", "Another Admin",
                    "password", "securepassword123",
                    "role", "ADMINISTRATOR"
            );

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Email already in use")));
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should reject invalid email format")
        void shouldRejectInvalidEmail() throws Exception {
            Map<String, String> request = Map.of(
                    "email", "invalid-email",
                    "fullName", "New User",
                    "password", "securepassword123",
                    "role", "OFFICER"
            );

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should reject short password")
        void shouldRejectShortPassword() throws Exception {
            Map<String, String> request = Map.of(
                    "email", "newuser@example.com",
                    "fullName", "New User",
                    "password", "short", // Too short
                    "role", "OFFICER"
            );

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should reject invalid role")
        void shouldRejectInvalidRole() throws Exception {
            Map<String, String> request = Map.of(
                    "email", "newuser@example.com",
                    "fullName", "New User",
                    "password", "securepassword123",
                    "role", "INVALID_ROLE"
            );

            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Invalid role")));
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should create user with all valid roles")
        void shouldCreateUserWithAllValidRoles() throws Exception {
            for (String role : new String[]{"ADMINISTRATOR", "OFFICER", "AUDITOR"}) {
                Map<String, String> request = Map.of(
                        "email", role.toLowerCase() + "_new@example.com",
                        "fullName", role + " New User",
                        "password", "securepassword123",
                        "role", role
                );

                mockMvc.perform(post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.roles", contains(role)));
            }
        }
    }

    @Nested
    @DisplayName("PUT /api/users/{id} - Update User")
    class UpdateUserTests {

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Administrator should update user")
        void adminShouldUpdateUser() throws Exception {
            Map<String, String> request = Map.of(
                    "email", "updated@example.com",
                    "fullName", "Updated User",
                    "role", "AUDITOR"
            );

            mockMvc.perform(put("/api/users/" + officerUser.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email", is("updated@example.com")))
                    .andExpect(jsonPath("$.fullName", is("Updated User")))
                    .andExpect(jsonPath("$.roles", contains("AUDITOR")));
        }

        @Test
        @WithMockUser(roles = "OFFICER")
        @DisplayName("Officer should be forbidden to update user")
        void officerShouldBeForbidden() throws Exception {
            Map<String, String> request = Map.of(
                    "email", "updated@example.com",
                    "fullName", "Updated User",
                    "role", "AUDITOR"
            );

            mockMvc.perform(put("/api/users/" + officerUser.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should update password when provided")
        void shouldUpdatePassword() throws Exception {
            Map<String, String> request = Map.of(
                    "email", "officer@example.com",
                    "fullName", "Officer User",
                    "password", "newpassword12345",
                    "role", "OFFICER"
            );

            mockMvc.perform(put("/api/users/" + officerUser.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // Verify password was updated
            User updated = userRepository.findById(officerUser.getId()).orElseThrow();
            assert passwordEncoder.matches("newpassword12345", updated.getPasswordHash());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should not update password when not provided")
        void shouldNotUpdatePasswordWhenNotProvided() throws Exception {
            String originalHash = officerUser.getPasswordHash();

            Map<String, String> request = Map.of(
                    "email", "officer@example.com",
                    "fullName", "Officer Updated",
                    "role", "OFFICER"
            );

            mockMvc.perform(put("/api/users/" + officerUser.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            User updated = userRepository.findById(officerUser.getId()).orElseThrow();
            assert updated.getPasswordHash().equals(originalHash);
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should return 404 for non-existent user")
        void shouldReturn404ForNonExistent() throws Exception {
            Map<String, String> request = Map.of(
                    "email", "updated@example.com",
                    "fullName", "Updated User",
                    "role", "OFFICER"
            );

            mockMvc.perform(put("/api/users/" + UUID.randomUUID())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should reject duplicate email for different user")
        void shouldRejectDuplicateEmail() throws Exception {
            Map<String, String> request = Map.of(
                    "email", "admin@example.com", // Already taken by adminUser
                    "fullName", "Officer User",
                    "role", "OFFICER"
            );

            mockMvc.perform(put("/api/users/" + officerUser.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Email already in use")));
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should allow keeping same email")
        void shouldAllowKeepingSameEmail() throws Exception {
            Map<String, String> request = Map.of(
                    "email", "officer@example.com", // Same email
                    "fullName", "Officer Updated",
                    "role", "OFFICER"
            );

            mockMvc.perform(put("/api/users/" + officerUser.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fullName", is("Officer Updated")));
        }
    }

    @Nested
    @DisplayName("PUT /api/users/{id}/status - Update User Status")
    class UpdateUserStatusTests {

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Administrator should disable user")
        void adminShouldDisableUser() throws Exception {
            Map<String, Boolean> request = Map.of("enabled", false);

            mockMvc.perform(put("/api/users/" + officerUser.getId() + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled", is(false)));
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Administrator should enable user")
        void adminShouldEnableUser() throws Exception {
            // First disable the user
            officerUser.setActive(false);
            userRepository.save(officerUser);

            Map<String, Boolean> request = Map.of("enabled", true);

            mockMvc.perform(put("/api/users/" + officerUser.getId() + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.enabled", is(true)));
        }

        @Test
        @WithMockUser(roles = "OFFICER")
        @DisplayName("Officer should be forbidden to update status")
        void officerShouldBeForbidden() throws Exception {
            Map<String, Boolean> request = Map.of("enabled", false);

            mockMvc.perform(put("/api/users/" + officerUser.getId() + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "AUDITOR")
        @DisplayName("Auditor should be forbidden to update status")
        void auditorShouldBeForbidden() throws Exception {
            Map<String, Boolean> request = Map.of("enabled", false);

            mockMvc.perform(put("/api/users/" + officerUser.getId() + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should return 404 for non-existent user")
        void shouldReturn404ForNonExistent() throws Exception {
            Map<String, Boolean> request = Map.of("enabled", false);

            mockMvc.perform(put("/api/users/" + UUID.randomUUID() + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/users/{id} - Delete User")
    class DeleteUserTests {

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Administrator should delete user")
        void adminShouldDeleteUser() throws Exception {
            mockMvc.perform(delete("/api/users/" + officerUser.getId()))
                    .andExpect(status().isNoContent());

            // Verify user is deleted
            assert userRepository.findById(officerUser.getId()).isEmpty();
        }

        @Test
        @WithMockUser(roles = "OFFICER")
        @DisplayName("Officer should be forbidden to delete user")
        void officerShouldBeForbidden() throws Exception {
            mockMvc.perform(delete("/api/users/" + officerUser.getId()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "AUDITOR")
        @DisplayName("Auditor should be forbidden to delete user")
        void auditorShouldBeForbidden() throws Exception {
            mockMvc.perform(delete("/api/users/" + officerUser.getId()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Should return 404 for non-existent user")
        void shouldReturn404ForNonExistent() throws Exception {
            mockMvc.perform(delete("/api/users/" + UUID.randomUUID()))
                    .andExpect(status().isNotFound());
        }
    }
}
