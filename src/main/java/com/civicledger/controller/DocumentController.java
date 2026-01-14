package com.civicledger.controller;

import com.civicledger.dto.PagedResponse;
import com.civicledger.entity.AuditLog.ActionType;
import com.civicledger.entity.AuditLog.AuditStatus;
import com.civicledger.entity.Document;
import com.civicledger.entity.User;
import com.civicledger.repository.DocumentRepository;
import com.civicledger.security.ClearanceValidator;
import com.civicledger.service.AuditService;
import com.civicledger.service.EncryptionService;
import com.civicledger.service.EncryptionService.EncryptionResult;
import com.civicledger.service.HashingService;
import com.civicledger.service.StorageService;
import com.civicledger.service.AISummaryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@Slf4j
public class DocumentController {

    private final DocumentRepository documentRepository;
    private final AuditService auditService;
    private final EncryptionService encryptionService;
    private final HashingService hashingService;
    private final StorageService storageService;
    private final ClearanceValidator clearanceValidator;
    private final AISummaryService aiSummaryService;

    public DocumentController(
            DocumentRepository documentRepository,
            AuditService auditService,
            EncryptionService encryptionService,
            HashingService hashingService,
            StorageService storageService,
            ClearanceValidator clearanceValidator,
            AISummaryService aiSummaryService) {
        this.documentRepository = documentRepository;
        this.auditService = auditService;
        this.encryptionService = encryptionService;
        this.hashingService = hashingService;
        this.storageService = storageService;
        this.clearanceValidator = clearanceValidator;
        this.aiSummaryService = aiSummaryService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<DocumentDTO>> listDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal User user) {

        if (user == null) {
            log.warn("listDocuments called with null user - authentication may have failed");
            return ResponseEntity.status(401).build();
        }

        log.debug("listDocuments: user={}, clearance={}, page={}, size={}, search={}",
                user.getEmail(), user.getClearanceLevel(), page, size, search);

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());
        List<User.ClassificationLevel> allowedLevels = clearanceValidator.getAccessibleLevels(user.getClearanceLevel());

        log.debug("listDocuments: allowedLevels={}", allowedLevels);

        Page<Document> documents;
        if (search != null && !search.isBlank()) {
            // Filter search results by clearance level
            documents = documentRepository.searchByFilenameWithClearance(search, allowedLevels, pageRequest);
        } else {
            // Only show documents at or below user's clearance level
            documents = documentRepository.findByClassificationLevelIn(allowedLevels, pageRequest);
        }

        log.debug("listDocuments: found {} documents (total: {})",
                documents.getContent().size(), documents.getTotalElements());

        PagedResponse<DocumentDTO> response = PagedResponse.fromPage(documents, this::toDTO);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> getDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            HttpServletRequest request) {
        return documentRepository.findByIdAndDeletedFalse(id)
                .map(doc -> {
                    // Validate clearance before granting access
                    if (!clearanceValidator.hasAccess(user, doc)) {
                        auditService.log(
                                ActionType.DOCUMENT_ACCESS_DENIED,
                                "DOCUMENT",
                                id.toString(),
                                AuditStatus.DENIED,
                                String.format("Access denied: user clearance %s, required %s",
                                        user.getClearanceLevel(), doc.getClassificationLevel()),
                                getClientIp(request),
                                request.getHeader("User-Agent")
                        );
                        clearanceValidator.validateAccess(user, doc); // Throws exception
                    }

                    auditService.log(
                            ActionType.DOCUMENT_VIEW,
                            "DOCUMENT",
                            id.toString(),
                            AuditStatus.SUCCESS,
                            "Viewed document: " + doc.getOriginalFilename(),
                            getClientIp(request),
                            request.getHeader("User-Agent")
                    );
                    return ResponseEntity.ok(toDTO(doc));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<StreamingResponseBody> downloadDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            HttpServletRequest request) {
        return documentRepository.findByIdAndDeletedFalse(id)
                .map(doc -> {
                    // Validate clearance BEFORE any decryption operations (fail fast)
                    if (!clearanceValidator.hasAccess(user, doc)) {
                        auditService.log(
                                ActionType.DOCUMENT_ACCESS_DENIED,
                                "DOCUMENT",
                                id.toString(),
                                AuditStatus.DENIED,
                                String.format("Download denied: user clearance %s, required %s",
                                        user.getClearanceLevel(), doc.getClassificationLevel()),
                                getClientIp(request),
                                request.getHeader("User-Agent")
                        );
                        clearanceValidator.validateAccess(user, doc); // Throws exception
                    }

                    try {
                        // Step 1: Check if file exists in storage
                        if (!storageService.exists(doc.getStoragePath())) {
                            log.error("Document file not found in storage: {} (path: {})",
                                    id, doc.getStoragePath());
                            auditService.log(
                                    ActionType.DOCUMENT_DOWNLOAD,
                                    "DOCUMENT",
                                    id.toString(),
                                    AuditStatus.FAILURE,
                                    "File not found in storage: " + doc.getOriginalFilename(),
                                    getClientIp(request),
                                    request.getHeader("User-Agent")
                            );
                            return ResponseEntity.notFound().<StreamingResponseBody>build();
                        }

                        // Step 2: Retrieve encrypted data from storage
                        byte[] encryptedData = storageService.retrieve(doc.getStoragePath());
                        log.debug("Retrieved {} bytes of encrypted data for document {}",
                                encryptedData.length, id);

                        // Step 3: Decrypt the data
                        byte[] iv = encryptionService.decodeFromBase64(doc.getEncryptionIv());
                        byte[] decryptedData = encryptionService.decrypt(encryptedData, iv);
                        log.debug("Decrypted to {} bytes", decryptedData.length);

                        // Step 4: Verify integrity with SHA-256 hash
                        String computedHash = hashingService.hash(decryptedData);
                        if (!hashingService.verify(decryptedData, doc.getFileHash())) {
                            log.error("Hash verification failed for document {}. Expected: {}, Got: {}",
                                    id, doc.getFileHash(), computedHash);
                            auditService.log(
                                    ActionType.DOCUMENT_DOWNLOAD,
                                    "DOCUMENT",
                                    id.toString(),
                                    AuditStatus.FAILURE,
                                    "INTEGRITY VIOLATION: Hash mismatch detected for " + doc.getOriginalFilename(),
                                    getClientIp(request),
                                    request.getHeader("User-Agent")
                            );
                            return ResponseEntity.status(500).<StreamingResponseBody>body(null);
                        }

                        // Capture values for use in lambda
                        String clientIp = getClientIp(request);
                        String userAgent = request.getHeader("User-Agent");
                        String filename = doc.getOriginalFilename();

                        // Log download started
                        auditService.log(
                                ActionType.DOCUMENT_DOWNLOAD_STARTED,
                                "DOCUMENT",
                                id.toString(),
                                AuditStatus.SUCCESS,
                                "Download started: " + filename + " (" + decryptedData.length + " bytes)",
                                clientIp,
                                userAgent
                        );

                        // Create streaming response to track transfer completion
                        StreamingResponseBody stream = outputStream -> {
                            try {
                                outputStream.write(decryptedData);
                                outputStream.flush();
                                // Log completed AFTER all bytes written to client
                                auditService.log(
                                        ActionType.DOCUMENT_DOWNLOAD_COMPLETED,
                                        "DOCUMENT",
                                        id.toString(),
                                        AuditStatus.SUCCESS,
                                        "Download completed: " + filename + " (hash verified)",
                                        clientIp,
                                        userAgent
                                );
                                log.debug("Download completed for document {}", id);
                            } catch (IOException e) {
                                // Connection dropped mid-transfer
                                log.warn("Download interrupted for document {}: {}", id, e.getMessage());
                                auditService.log(
                                        ActionType.DOCUMENT_DOWNLOAD,
                                        "DOCUMENT",
                                        id.toString(),
                                        AuditStatus.FAILURE,
                                        "Download interrupted: " + filename + " - " + e.getMessage(),
                                        clientIp,
                                        userAgent
                                );
                                throw e;
                            }
                        };

                        String contentType = doc.getContentType() != null
                                ? doc.getContentType()
                                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

                        return ResponseEntity.ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION,
                                        "attachment; filename=\"" + filename + "\"")
                                .contentType(MediaType.parseMediaType(contentType))
                                .contentLength(decryptedData.length)
                                .body(stream);

                    } catch (Exception e) {
                        log.error("Failed to download document {}: {}", id, e.getMessage(), e);
                        auditService.log(
                                ActionType.DOCUMENT_DOWNLOAD,
                                "DOCUMENT",
                                id.toString(),
                                AuditStatus.FAILURE,
                                "Failed to download document: " + e.getMessage(),
                                getClientIp(request),
                                request.getHeader("User-Agent")
                        );
                        return ResponseEntity.internalServerError().<StreamingResponseBody>build();
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/upload")
    public ResponseEntity<DocumentDTO> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String classificationLevel,
            @AuthenticationPrincipal User user,
            HttpServletRequest request) {

        try {
            // Read file bytes
            byte[] fileBytes = file.getBytes();

            // Step 1: Calculate SHA-256 hash of original file (for integrity verification)
            String fileHash = hashingService.hash(fileBytes);
            log.debug("Calculated SHA-256 hash for {}: {}", file.getOriginalFilename(), fileHash);

            // Step 2: Encrypt file content with AES-256-GCM
            EncryptionResult encryptionResult = encryptionService.encrypt(fileBytes);
            log.debug("Encrypted {} bytes -> {} bytes ciphertext",
                    fileBytes.length, encryptionResult.ciphertext().length);

            // Step 3: Store encrypted data
            String storagePath = storageService.store(
                    encryptionResult.ciphertext(),
                    file.getOriginalFilename()
            );
            log.debug("Stored encrypted file at: {}", storagePath);

            // Step 4: Create document record with actual cryptographic values
            Document document = Document.builder()
                    .originalFilename(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .originalSize(file.getSize())
                    .encryptedSize((long) encryptionResult.ciphertext().length)
                    .fileHash(fileHash)
                    .encryptionIv(encryptionResult.ivBase64())
                    .storagePath(storagePath)
                    .uploadedBy(user.getId().toString())
                    .description(description)
                    .classificationLevel(classificationLevel != null
                            ? User.ClassificationLevel.valueOf(classificationLevel)
                            : User.ClassificationLevel.UNCLASSIFIED)
                    .createdAt(Instant.now())
                    .build();

            Document saved = documentRepository.save(document);

            // Log the upload
            auditService.log(
                    ActionType.DOCUMENT_UPLOAD,
                    "DOCUMENT",
                    saved.getId().toString(),
                    AuditStatus.SUCCESS,
                    String.format("Uploaded document: %s (size: %d bytes, hash: %s, classification: %s)",
                            file.getOriginalFilename(), file.getSize(), fileHash.substring(0, 16) + "...",
                            saved.getClassificationLevel()),
                    getClientIp(request),
                    request.getHeader("User-Agent")
            );

            return ResponseEntity.ok(toDTO(saved));

        } catch (IOException e) {
            log.error("Failed to read uploaded file: {}", file.getOriginalFilename(), e);
            auditService.log(
                    ActionType.DOCUMENT_UPLOAD,
                    "DOCUMENT",
                    null,
                    AuditStatus.FAILURE,
                    "Failed to upload document: " + file.getOriginalFilename() + " - " + e.getMessage(),
                    getClientIp(request),
                    request.getHeader("User-Agent")
            );
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            HttpServletRequest request) {

        return documentRepository.findByIdAndDeletedFalse(id)
                .map(doc -> {
                    String filename = doc.getOriginalFilename();
                    doc.markDeleted(user.getId().toString());
                    documentRepository.save(doc);

                    // Log the deletion
                    auditService.log(
                            ActionType.DOCUMENT_DELETE,
                            "DOCUMENT",
                            id.toString(),
                            AuditStatus.SUCCESS,
                            "Deleted document: " + filename,
                            getClientIp(request),
                            request.getHeader("User-Agent")
                    );

                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Generate AI summary for a document.
     * Only works for text-based documents.
     */
    @PostMapping("/{id}/summary")
    public ResponseEntity<?> generateSummary(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user,
            HttpServletRequest request) {

        if (!aiSummaryService.isAvailable()) {
            return ResponseEntity.status(503)
                    .body(new ErrorResponse("AI summary service is not available"));
        }

        return documentRepository.findByIdAndDeletedFalse(id)
                .map(doc -> {
                    // Validate clearance
                    if (!clearanceValidator.hasAccess(user, doc)) {
                        return ResponseEntity.status(403)
                                .body(new ErrorResponse("Access denied"));
                    }

                    if (!aiSummaryService.isSummarizable(doc.getContentType())) {
                        return ResponseEntity.badRequest()
                                .body(new ErrorResponse("Document type not supported for AI summary"));
                    }

                    var summary = aiSummaryService.generateSummary(id);

                    if (summary.isPresent()) {
                        auditService.log(
                                ActionType.DOCUMENT_VIEW,
                                "DOCUMENT",
                                id.toString(),
                                AuditStatus.SUCCESS,
                                "Generated AI summary for: " + doc.getOriginalFilename(),
                                getClientIp(request),
                                request.getHeader("User-Agent")
                        );

                        // Refresh document from DB to get updated summary
                        Document updated = documentRepository.findById(id).orElse(doc);
                        return ResponseEntity.ok(toDTO(updated));
                    } else {
                        return ResponseEntity.status(500)
                                .body(new ErrorResponse("Failed to generate summary"));
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    record ErrorResponse(String message) {}

    private DocumentDTO toDTO(Document doc) {
        return new DocumentDTO(
                doc.getId().toString(),
                doc.getOriginalFilename(),
                doc.getFileHash(),
                doc.getClassificationLevel().name(),
                doc.getVersionNumber(),
                doc.getUploadedBy(),
                doc.getCreatedAt().toString(),
                doc.getOriginalSize(),
                doc.getAiSummary(),
                doc.getSummaryGeneratedAt() != null ? doc.getSummaryGeneratedAt().toString() : null,
                aiSummaryService.isSummarizable(doc.getContentType())
        );
    }

    public record DocumentDTO(
            String id,
            String fileName,
            String fileHash,
            String classificationLevel,
            Integer versionNumber,
            String uploadedBy,
            String uploadedAt,
            Long fileSize,
            String aiSummary,
            String summaryGeneratedAt,
            boolean canGenerateSummary
    ) {}

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
