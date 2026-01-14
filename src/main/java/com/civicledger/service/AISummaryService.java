package com.civicledger.service;

import com.civicledger.entity.Document;
import com.civicledger.repository.DocumentRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AI-powered document summarization service using OpenAI API.
 * Generates concise one-sentence summaries of document content.
 */
@Service
@Slf4j
public class AISummaryService {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String SYSTEM_PROMPT =
            "You are a document summarizer for a government document management system. " +
            "Generate a single, concise sentence summarizing the key content of the document. " +
            "Be factual and professional. Do not include any sensitive information in the summary.";

    private final RestClient restClient;
    private final DocumentRepository documentRepository;
    private final StorageService storageService;
    private final EncryptionService encryptionService;

    private final boolean enabled;
    private final String apiKey;
    private final String model;
    private final int maxTokens;

    public AISummaryService(
            DocumentRepository documentRepository,
            StorageService storageService,
            EncryptionService encryptionService,
            @Value("${civicledger.ai.enabled:false}") boolean enabled,
            @Value("${civicledger.ai.openai.api-key:}") String apiKey,
            @Value("${civicledger.ai.openai.model:gpt-4o-mini}") String model,
            @Value("${civicledger.ai.openai.max-tokens:100}") int maxTokens) {

        this.documentRepository = documentRepository;
        this.storageService = storageService;
        this.encryptionService = encryptionService;
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;

        this.restClient = RestClient.builder()
                .baseUrl(OPENAI_API_URL)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();

        if (enabled && (apiKey == null || apiKey.isBlank())) {
            log.warn("AI summary is enabled but no OpenAI API key configured. Set OPENAI_API_KEY environment variable.");
        } else if (enabled) {
            log.info("AI Summary Service initialized with model: {}", model);
        } else {
            log.info("AI Summary Service is disabled");
        }
    }

    /**
     * Generates an AI summary for the specified document.
     *
     * @param documentId The document UUID to summarize
     * @return The generated summary, or empty if unavailable
     */
    public Optional<String> generateSummary(UUID documentId) {
        if (!enabled) {
            log.debug("AI summary generation is disabled");
            return Optional.empty();
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Cannot generate summary: OpenAI API key not configured");
            return Optional.empty();
        }

        Optional<Document> docOpt = documentRepository.findByIdAndDeletedFalse(documentId);
        if (docOpt.isEmpty()) {
            log.warn("Document not found: {}", documentId);
            return Optional.empty();
        }

        Document document = docOpt.get();

        // Only summarize text-based documents
        if (!isTextDocument(document.getContentType())) {
            log.debug("Skipping non-text document: {} ({})", documentId, document.getContentType());
            return Optional.empty();
        }

        try {
            // Retrieve and decrypt document content
            String content = extractDocumentContent(document);
            if (content == null || content.isBlank()) {
                log.warn("Document has no extractable content: {}", documentId);
                return Optional.empty();
            }

            // Truncate content if too long (OpenAI has token limits)
            String truncatedContent = truncateContent(content, 4000);

            // Call OpenAI API
            String summary = callOpenAI(truncatedContent);

            if (summary != null && !summary.isBlank()) {
                // Save summary to document
                document.setAiSummary(summary);
                document.setSummaryGeneratedAt(Instant.now());
                documentRepository.save(document);

                log.info("Generated AI summary for document: {}", documentId);
                return Optional.of(summary);
            }

        } catch (Exception e) {
            log.error("Failed to generate AI summary for document {}: {}", documentId, e.getMessage(), e);
        }

        return Optional.empty();
    }

    /**
     * Checks if the document is AI-summarizable (text-based).
     */
    public boolean isSummarizable(String contentType) {
        return isTextDocument(contentType);
    }

    /**
     * Checks if AI summary feature is enabled and configured.
     */
    public boolean isAvailable() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    private boolean isTextDocument(String contentType) {
        if (contentType == null) return false;
        return contentType.startsWith("text/") ||
               contentType.equals("application/json") ||
               contentType.equals("application/xml") ||
               contentType.contains("markdown");
    }

    private String extractDocumentContent(Document document) {
        try {
            // Retrieve encrypted content
            byte[] encryptedData = storageService.retrieve(document.getStoragePath());

            // Decrypt content
            byte[] iv = encryptionService.decodeFromBase64(document.getEncryptionIv());
            byte[] decryptedData = encryptionService.decrypt(encryptedData, iv);

            // Convert to string (assuming UTF-8)
            return new String(decryptedData, java.nio.charset.StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Failed to extract document content: {}", e.getMessage());
            return null;
        }
    }

    private String truncateContent(String content, int maxChars) {
        if (content.length() <= maxChars) {
            return content;
        }
        return content.substring(0, maxChars) + "... [truncated]";
    }

    private String callOpenAI(String documentContent) {
        ChatRequest request = new ChatRequest(
                model,
                List.of(
                        new Message("system", SYSTEM_PROMPT),
                        new Message("user", "Summarize this document in one sentence:\n\n" + documentContent)
                ),
                maxTokens
        );

        try {
            ChatResponse response = restClient.post()
                    .header("Authorization", "Bearer " + apiKey)
                    .body(request)
                    .retrieve()
                    .body(ChatResponse.class);

            if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                return response.choices().get(0).message().content().trim();
            }

        } catch (Exception e) {
            log.error("OpenAI API call failed: {}", e.getMessage());
            throw e;
        }

        return null;
    }

    // OpenAI API DTOs
    record ChatRequest(
            String model,
            List<Message> messages,
            @JsonProperty("max_tokens") int maxTokens
    ) {}

    record Message(String role, String content) {}

    record ChatResponse(
            String id,
            List<Choice> choices
    ) {}

    record Choice(
            int index,
            Message message
    ) {}
}
