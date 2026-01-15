package com.civicledger.service;

import com.civicledger.entity.Document;
import com.civicledger.exception.StorageException;
import com.civicledger.repository.DocumentRepository;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AI-powered document summarization service using OpenAI API.
 * Supports text documents, PDFs (text extraction), and images (vision API).
 */
@Service
@Slf4j
public class AISummaryService {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String SYSTEM_PROMPT =
            "You are a document summarizer for a government document management system. " +
            "Generate a single, concise sentence summarizing the key content of the document. " +
            "Be factual and professional. Do not include any sensitive information in the summary.";
    private static final String IMAGE_PROMPT =
            "Describe what this image contains in one concise sentence. " +
            "Be factual and professional. Focus on the main subject or content.";

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
        String contentType = document.getContentType();

        if (!isSummarizable(contentType)) {
            log.debug("Document type not supported for summarization: {} ({})", documentId, contentType);
            return Optional.empty();
        }

        try {
            byte[] decryptedData = decryptDocument(document);
            if (decryptedData == null) {
                return Optional.empty();
            }

            String summary;
            if (isImageDocument(contentType)) {
                summary = summarizeImage(decryptedData, contentType);
            } else if (isPdfDocument(contentType)) {
                summary = summarizePdf(decryptedData);
            } else {
                summary = summarizeText(decryptedData);
            }

            if (summary != null && !summary.isBlank()) {
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
     * Checks if the document type is supported for AI summarization.
     */
    public boolean isSummarizable(String contentType) {
        return isTextDocument(contentType) || isPdfDocument(contentType) || isImageDocument(contentType);
    }

    /**
     * Checks if AI summary feature is enabled and configured.
     */
    public boolean isAvailable() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    private byte[] decryptDocument(Document document) {
        try {
            // Check if file exists first
            if (!storageService.exists(document.getStoragePath())) {
                log.error("Document file not found in storage: {} (path: {})",
                        document.getId(), document.getStoragePath());
                markDocumentUnavailable(document, Document.IntegrityStatus.MISSING);
                return null;
            }

            byte[] encryptedData = storageService.retrieve(document.getStoragePath());
            byte[] iv = encryptionService.decodeFromBase64(document.getEncryptionIv());
            return encryptionService.decrypt(encryptedData, iv);
        } catch (StorageException e) {
            log.error("Storage error for document {}: {}", document.getId(), e.getMessage());
            markDocumentUnavailable(document, Document.IntegrityStatus.CORRUPTED);
            return null;
        } catch (Exception e) {
            log.error("Failed to decrypt document {}: {}", document.getId(), e.getMessage());
            markDocumentUnavailable(document, Document.IntegrityStatus.CORRUPTED);
            return null;
        }
    }

    /**
     * Marks a document as unavailable due to storage/integrity issues.
     */
    private void markDocumentUnavailable(Document document, Document.IntegrityStatus status) {
        document.setIntegrityStatus(status);
        documentRepository.save(document);
        log.warn("Marked document {} as {}", document.getId(), status);
    }

    private String summarizeText(byte[] data) {
        String content = new String(data, java.nio.charset.StandardCharsets.UTF_8);
        String truncated = truncateContent(content, 4000);
        return callOpenAIText(truncated);
    }

    private String summarizePdf(byte[] data) {
        try {
            String text = extractPdfText(data);
            if (text == null || text.isBlank()) {
                log.warn("PDF has no extractable text");
                return null;
            }
            String truncated = truncateContent(text, 4000);
            return callOpenAIText(truncated);
        } catch (Exception e) {
            log.error("Failed to extract PDF text: {}", e.getMessage());
            return null;
        }
    }

    private String summarizeImage(byte[] data, String contentType) {
        String base64Image = Base64.getEncoder().encodeToString(data);
        String mediaType = contentType.startsWith("image/") ? contentType : "image/png";
        return callOpenAIVision(base64Image, mediaType);
    }

    private String extractPdfText(byte[] pdfData) throws Exception {
        // Validate PDF structure before attempting extraction
        if (!isValidPdf(pdfData)) {
            throw new IllegalArgumentException("Invalid or corrupted PDF file");
        }

        try (PDDocument document = Loader.loadPDF(pdfData)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * Validates that the byte array represents a valid PDF file.
     * Checks for PDF magic number and basic structure.
     */
    private boolean isValidPdf(byte[] data) {
        if (data == null || data.length < 8) {
            return false;
        }

        // Check for PDF magic number: %PDF-
        if (data[0] != 0x25 || data[1] != 0x50 || data[2] != 0x44 || data[3] != 0x46 || data[4] != 0x2D) {
            log.warn("PDF validation failed: missing PDF header signature");
            return false;
        }

        // Check for %%EOF marker (should be near the end)
        String tail = new String(data, Math.max(0, data.length - 1024), Math.min(1024, data.length));
        if (!tail.contains("%%EOF")) {
            log.warn("PDF validation failed: missing EOF marker - file may be truncated");
            return false;
        }

        return true;
    }

    private boolean isTextDocument(String contentType) {
        if (contentType == null) return false;
        return contentType.startsWith("text/") ||
               contentType.equals("application/json") ||
               contentType.equals("application/xml") ||
               contentType.contains("markdown");
    }

    private boolean isPdfDocument(String contentType) {
        return contentType != null && contentType.equals("application/pdf");
    }

    private boolean isImageDocument(String contentType) {
        if (contentType == null) return false;
        return contentType.equals("image/png") ||
               contentType.equals("image/jpeg") ||
               contentType.equals("image/gif") ||
               contentType.equals("image/webp");
    }

    private String truncateContent(String content, int maxChars) {
        if (content.length() <= maxChars) {
            return content;
        }
        return content.substring(0, maxChars) + "... [truncated]";
    }

    private String callOpenAIText(String documentContent) {
        ChatRequest request = new ChatRequest(
                model,
                List.of(
                        new TextMessage("system", SYSTEM_PROMPT),
                        new TextMessage("user", "Summarize this document in one sentence:\n\n" + documentContent)
                ),
                maxTokens
        );

        return executeOpenAIRequest(request);
    }

    private String callOpenAIVision(String base64Image, String mediaType) {
        ImageUrl imageUrl = new ImageUrl("data:" + mediaType + ";base64," + base64Image);
        ImageContent imageContent = new ImageContent("image_url", null, imageUrl);
        TextContent textContent = new TextContent("text", IMAGE_PROMPT, null);

        VisionMessage userMessage = new VisionMessage("user", List.of(textContent, imageContent));

        ChatRequest request = new ChatRequest(
                model,
                List.of(
                        new TextMessage("system", SYSTEM_PROMPT),
                        userMessage
                ),
                maxTokens
        );

        return executeOpenAIRequest(request);
    }

    private String executeOpenAIRequest(ChatRequest request) {
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
            List<? extends Object> messages,
            @JsonProperty("max_tokens") int maxTokens
    ) {}

    record TextMessage(String role, String content) {}

    record VisionMessage(String role, List<? extends Content> content) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    interface Content {
        String type();
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record TextContent(String type, String text, ImageUrl image_url) implements Content {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ImageContent(String type, String text, ImageUrl image_url) implements Content {}

    record ImageUrl(String url) {}

    record ChatResponse(
            String id,
            List<Choice> choices
    ) {}

    record Choice(
            int index,
            ResponseMessage message
    ) {}

    record ResponseMessage(String role, String content) {}
}
