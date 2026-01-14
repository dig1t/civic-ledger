package com.civicledger.repository;

import com.civicledger.entity.Role;
import com.civicledger.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User entity operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by Keycloak subject ID.
     */
    Optional<User> findByKeycloakId(String keycloakId);

    /**
     * Find user by email address.
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if user exists by email.
     */
    boolean existsByEmail(String email);

    /**
     * Check if user exists by Keycloak ID.
     */
    boolean existsByKeycloakId(String keycloakId);

    /**
     * Find all active users.
     */
    List<User> findByActiveTrue();

    /**
     * Find all users with a specific role.
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role AND u.active = true")
    List<User> findByRole(@Param("role") Role role);

    /**
     * Find all administrators.
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = 'ADMINISTRATOR' AND u.active = true")
    List<User> findAdministrators();

    /**
     * Count active users.
     */
    long countByActiveTrue();

    /**
     * Find all users with pagination.
     */
    Page<User> findAll(Pageable pageable);

    /**
     * Search users by name or email (case-insensitive).
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<User> searchUsers(@Param("search") String search, Pageable pageable);

    /**
     * Filter users by role with pagination.
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role")
    Page<User> findByRolePaginated(@Param("role") Role role, Pageable pageable);

    /**
     * Filter users by clearance level with pagination.
     */
    Page<User> findByClearanceLevel(User.ClassificationLevel clearanceLevel, Pageable pageable);

    /**
     * Filter users by role and clearance level with pagination.
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role AND u.clearanceLevel = :clearanceLevel")
    Page<User> findByRoleAndClearanceLevel(
            @Param("role") Role role,
            @Param("clearanceLevel") User.ClassificationLevel clearanceLevel,
            Pageable pageable);

    /**
     * Search users with role filter.
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role AND " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchUsersByRole(@Param("search") String search, @Param("role") Role role, Pageable pageable);

    /**
     * Search users with clearance level filter.
     */
    @Query("SELECT u FROM User u WHERE u.clearanceLevel = :clearanceLevel AND " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchUsersByClearanceLevel(
            @Param("search") String search,
            @Param("clearanceLevel") User.ClassificationLevel clearanceLevel,
            Pageable pageable);

    /**
     * Search users with role and clearance level filters.
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role AND u.clearanceLevel = :clearanceLevel AND " +
           "(LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> searchUsersByRoleAndClearanceLevel(
            @Param("search") String search,
            @Param("role") Role role,
            @Param("clearanceLevel") User.ClassificationLevel clearanceLevel,
            Pageable pageable);
}
