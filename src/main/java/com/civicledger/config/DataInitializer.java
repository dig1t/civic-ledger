package com.civicledger.config;

import com.civicledger.entity.Role;
import com.civicledger.entity.User;
import com.civicledger.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

/**
 * Initializes test data for development environment.
 * Only runs in 'dev' profile.
 */
@Configuration
@Profile("dev")
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initTestUsers(UserRepository userRepository,
                                     PasswordEncoder passwordEncoder) {
        return args -> {
            // Only seed if no users exist
            if (userRepository.count() > 0) {
                log.info("Users already exist, skipping seed data");
                return;
            }

            log.info("Seeding test users for development...");

            // Administrator user
            User admin = User.builder()
                    .email("admin@civicledger.gov")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .firstName("System")
                    .lastName("Administrator")
                    .keycloakId("local-admin")
                    .roles(Set.of(Role.ADMINISTRATOR))
                    .clearanceLevel(User.ClassificationLevel.TOP_SECRET)
                    .active(true)
                    .build();
            userRepository.save(admin);
            log.info("Created admin user: {}", admin.getEmail());

            // Officer user
            User officer = User.builder()
                    .email("officer@civicledger.gov")
                    .passwordHash(passwordEncoder.encode("officer123"))
                    .firstName("Jane")
                    .lastName("Officer")
                    .keycloakId("local-officer")
                    .roles(Set.of(Role.OFFICER))
                    .clearanceLevel(User.ClassificationLevel.SECRET)
                    .active(true)
                    .build();
            userRepository.save(officer);
            log.info("Created officer user: {}", officer.getEmail());

            // Auditor user
            User auditor = User.builder()
                    .email("auditor@civicledger.gov")
                    .passwordHash(passwordEncoder.encode("auditor123"))
                    .firstName("John")
                    .lastName("Auditor")
                    .keycloakId("local-auditor")
                    .roles(Set.of(Role.AUDITOR))
                    .clearanceLevel(User.ClassificationLevel.SECRET)
                    .active(true)
                    .build();
            userRepository.save(auditor);
            log.info("Created auditor user: {}", auditor.getEmail());

            log.info("Test user seeding complete!");
            log.info("========================================");
            log.info("TEST CREDENTIALS (dev profile only):");
            log.info("  Admin:   admin@civicledger.gov / admin123");
            log.info("  Officer: officer@civicledger.gov / officer123");
            log.info("  Auditor: auditor@civicledger.gov / auditor123");
            log.info("========================================");
        };
    }
}
