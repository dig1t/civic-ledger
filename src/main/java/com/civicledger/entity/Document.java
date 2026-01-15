package com.civicledger.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Document entity for secure document management.
 * Stores metadata about encrypted files with integrity verification.
 */
@Entity
@Table(name = "documents", indexes = {
    @Index(name = "idx_document_file_hash", columnList = "fileHash"),
    @Index(name = "idx_document_uploaded_by", columnList = "uploadedBy"),
    @Index(name = "idx_document_classification", columnList = "classificationLevel"),
    @Index(name = "idx_document_created_at", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Original filename (display purposes only).
     */
    @Column(nullable = false)
    private String originalFilename;

    /**
     * MIME type of the document.
     */
    @Column(nullable = false)
    private String contentType;

    /**
     * Size of the original unencrypted file in bytes.
     */
    @Column(nullable = false)
    private Long originalSize;

    /**
     * Size of the encrypted file in bytes.
     */
    @Column(nullable = false)
    private Long encryptedSize;

    /**
     * SHA-256 hash of the original file content for integrity verification.
     */
    @Column(nullable = false, length = 64)
    private String fileHash;

    /**
     * Base64-encoded IV used for AES-GCM encryption.
     */
    @Column(nullable = false)
    private String encryptionIv;

    /**
     * Path/key to the encrypted file in storage.
     */
    @Column(nullable = false)
    private String storagePath;

    /**
     * Version number for document versioning.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer versionNumber = 1;

    /**
     * Reference to parent document if this is a new version.
     */
    @Column
    private UUID parentVersionId;

    /**
     * Classification level for access control.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private User.ClassificationLevel classificationLevel = User.ClassificationLevel.UNCLASSIFIED;

    /**
     * User ID who uploaded the document.
     */
    @Column(nullable = false)
    private String uploadedBy;

    /**
     * Timestamp of upload.
     */
    @Column(nullable = false)
    private Instant createdAt;

    /**
     * Timestamp of last metadata update.
     */
    @Column
    private Instant updatedAt;

    /**
     * Optional description of the document.
     */
    @Column(length = 2000)
    private String description;

    /**
     * AI-generated summary of the document content.
     */
    @Column(length = 500)
    private String aiSummary;

    /**
     * Timestamp when AI summary was generated.
     */
    @Column
    private Instant summaryGeneratedAt;

    /**
     * Optional tags for categorization (comma-separated).
     */
    @Column(length = 500)
    private String tags;

    /**
     * Whether the document is marked for deletion (soft delete).
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean deleted = false;

    /**
     * Timestamp of soft deletion.
     */
    @Column
    private Instant deletedAt;

    /**
     * User ID who deleted the document.
     */
    @Column
    private String deletedBy;

    /**
     * Optional retention policy (e.g., "7_YEARS", "PERMANENT").
     */
    @Column
    private String retentionPolicy;

    /**
     * Expiration date based on retention policy.
     */
    @Column
    private Instant expiresAt;

    /**
     * File integrity status for tracking corrupted/missing files.
     * Null is treated as VALID for backwards compatibility.
     */
    @Enumerated(EnumType.STRING)
    @Column
    @Builder.Default
    private IntegrityStatus integrityStatus = IntegrityStatus.VALID;

    /**
     * Integrity status values for document files.
     */
    public enum IntegrityStatus {
        VALID,           // File exists and hash verified
        CORRUPTED,       // Hash verification failed
        MISSING          // File not found in storage
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Marks the document as deleted (soft delete).
     */
    public void markDeleted(String deletedByUserId) {
        this.deleted = true;
        this.deletedAt = Instant.now();
        this.deletedBy = deletedByUserId;
    }

    /**
     * Creates a new version of this document.
     */
    public Document createNewVersion() {
        return Document.builder()
                .originalFilename(this.originalFilename)
                .contentType(this.contentType)
                .classificationLevel(this.classificationLevel)
                .description(this.description)
                .tags(this.tags)
                .retentionPolicy(this.retentionPolicy)
                .versionNumber(this.versionNumber + 1)
                .parentVersionId(this.id)
                .build();
    }
}
