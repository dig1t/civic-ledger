package com.civicledger.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Entity for caching AI-generated summaries by file hash.
 * Prevents redundant OpenAI API calls for duplicate files.
 */
@Entity
@Table(name = "ai_summaries", indexes = {
    @Index(name = "idx_ai_summary_file_hash", columnList = "fileHash", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AISummary {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * SHA-256 hash of the file content (unique key for caching).
     */
    @Column(nullable = false, unique = true, length = 64)
    private String fileHash;

    /**
     * AI-generated summary text.
     */
    @Column(nullable = false, length = 500)
    private String summary;

    /**
     * MIME type of the summarized content (for reference).
     */
    @Column(length = 100)
    private String contentType;

    /**
     * OpenAI model used to generate the summary.
     */
    @Column(length = 50)
    private String modelUsed;

    /**
     * Timestamp when the summary was generated.
     */
    @Column(nullable = false)
    private Instant generatedAt;

    /**
     * Record creation timestamp.
     */
    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (generatedAt == null) {
            generatedAt = Instant.now();
        }
    }
}
