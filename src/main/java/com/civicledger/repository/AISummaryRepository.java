package com.civicledger.repository;

import com.civicledger.entity.AISummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for AI summary caching operations.
 */
@Repository
public interface AISummaryRepository extends JpaRepository<AISummary, UUID> {

    /**
     * Find cached summary by file hash.
     */
    Optional<AISummary> findByFileHash(String fileHash);

    /**
     * Check if a summary exists for the given file hash.
     */
    boolean existsByFileHash(String fileHash);
}
