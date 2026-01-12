package com.civicledger.repository;

import com.civicledger.entity.Role;
import com.civicledger.entity.User;
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
}
