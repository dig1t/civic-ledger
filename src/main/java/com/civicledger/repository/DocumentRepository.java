package com.civicledger.repository;

import com.civicledger.entity.Document;
import com.civicledger.entity.User.ClassificationLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Document entity operations.
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    /**
     * Find document by ID (excluding deleted).
     */
    Optional<Document> findByIdAndDeletedFalse(UUID id);

    /**
     * Find all documents (excluding deleted) with pagination.
     */
    Page<Document> findByDeletedFalse(Pageable pageable);

    /**
     * Find documents by uploader.
     */
    Page<Document> findByUploadedByAndDeletedFalse(String uploadedBy, Pageable pageable);

    /**
     * Find documents by classification level.
     */
    Page<Document> findByClassificationLevelAndDeletedFalse(
            ClassificationLevel level, Pageable pageable);

    /**
     * Find documents with classification at or below a given level.
     */
    @Query("SELECT d FROM Document d WHERE d.deleted = false AND d.classificationLevel <= :maxLevel ORDER BY d.createdAt DESC")
    Page<Document> findByClassificationLevelAtOrBelow(
            @Param("maxLevel") ClassificationLevel maxLevel, Pageable pageable);

    /**
     * Find document by file hash (for deduplication).
     */
    Optional<Document> findByFileHashAndDeletedFalse(String fileHash);

    /**
     * Check if a document with the given hash already exists.
     */
    boolean existsByFileHashAndDeletedFalse(String fileHash);

    /**
     * Find all versions of a document.
     */
    @Query("SELECT d FROM Document d WHERE (d.id = :documentId OR d.parentVersionId = :documentId) AND d.deleted = false ORDER BY d.versionNumber ASC")
    List<Document> findAllVersions(@Param("documentId") UUID documentId);

    /**
     * Find the latest version of a document.
     */
    @Query("SELECT d FROM Document d WHERE (d.id = :documentId OR d.parentVersionId = :documentId) AND d.deleted = false ORDER BY d.versionNumber DESC LIMIT 1")
    Optional<Document> findLatestVersion(@Param("documentId") UUID documentId);

    /**
     * Search documents by filename.
     */
    @Query("SELECT d FROM Document d WHERE d.deleted = false AND LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Document> searchByFilename(@Param("query") String query, Pageable pageable);

    /**
     * Find documents created within a date range.
     */
    @Query("SELECT d FROM Document d WHERE d.deleted = false AND d.createdAt BETWEEN :start AND :end ORDER BY d.createdAt DESC")
    Page<Document> findByCreatedAtBetween(
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable);

    /**
     * Find expired documents for cleanup.
     */
    @Query("SELECT d FROM Document d WHERE d.deleted = false AND d.expiresAt IS NOT NULL AND d.expiresAt < :now")
    List<Document> findExpiredDocuments(@Param("now") Instant now);

    /**
     * Find soft-deleted documents older than a given date (for permanent deletion).
     */
    @Query("SELECT d FROM Document d WHERE d.deleted = true AND d.deletedAt < :before")
    List<Document> findDeletedDocumentsOlderThan(@Param("before") Instant before);

    /**
     * Count documents by classification level.
     */
    @Query("SELECT d.classificationLevel, COUNT(d) FROM Document d WHERE d.deleted = false GROUP BY d.classificationLevel")
    List<Object[]> countByClassificationLevel();

    /**
     * Get total storage used.
     */
    @Query("SELECT SUM(d.encryptedSize) FROM Document d WHERE d.deleted = false")
    Long getTotalStorageUsed();
}
