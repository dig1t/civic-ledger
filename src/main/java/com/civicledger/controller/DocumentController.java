package com.civicledger.controller;

import com.civicledger.entity.AuditLog.ActionType;
import com.civicledger.entity.AuditLog.AuditStatus;
import com.civicledger.entity.Document;
import com.civicledger.entity.User;
import com.civicledger.repository.DocumentRepository;
import com.civicledger.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentRepository documentRepository;
    private final AuditService auditService;

    public DocumentController(DocumentRepository documentRepository, AuditService auditService) {
        this.documentRepository = documentRepository;
        this.auditService = auditService;
    }

    @GetMapping
    public ResponseEntity<List<DocumentDTO>> listDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Document> documents;
        if (search != null && !search.isBlank()) {
            documents = documentRepository.searchByFilename(search, pageRequest);
        } else {
            documents = documentRepository.findByDeletedFalse(pageRequest);
        }

        List<DocumentDTO> dtos = documents.map(this::toDTO).getContent();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentDTO> getDocument(
            @PathVariable UUID id,
            HttpServletRequest request) {
        return documentRepository.findByIdAndDeletedFalse(id)
                .map(doc -> {
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
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable UUID id,
            HttpServletRequest request) {
        return documentRepository.findByIdAndDeletedFalse(id)
                .map(doc -> {
                    // Log the download
                    auditService.log(
                            ActionType.DOCUMENT_DOWNLOAD,
                            "DOCUMENT",
                            id.toString(),
                            AuditStatus.SUCCESS,
                            "Downloaded document: " + doc.getOriginalFilename(),
                            getClientIp(request),
                            request.getHeader("User-Agent")
                    );

                    // TODO: Implement actual file retrieval and decryption
                    // For now, return a placeholder response
                    String placeholderContent = String.format(
                            "CivicLedger Document Placeholder\n" +
                            "================================\n\n" +
                            "Document ID: %s\n" +
                            "Filename: %s\n" +
                            "Classification: %s\n" +
                            "Original Size: %d bytes\n\n" +
                            "Note: File storage and encryption not yet implemented.\n" +
                            "In production, this would return the decrypted document.\n",
                            doc.getId(),
                            doc.getOriginalFilename(),
                            doc.getClassificationLevel(),
                            doc.getOriginalSize()
                    );

                    ByteArrayResource resource = new ByteArrayResource(
                            placeholderContent.getBytes(StandardCharsets.UTF_8));

                    String filename = doc.getOriginalFilename();
                    String contentType = doc.getContentType() != null
                            ? doc.getContentType()
                            : MediaType.APPLICATION_OCTET_STREAM_VALUE;

                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION,
                                    "attachment; filename=\"" + filename + "\"")
                            .contentType(MediaType.parseMediaType(contentType))
                            .contentLength(resource.contentLength())
                            .body((Resource) resource);
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

        // TODO: Implement actual file encryption and storage
        // For now, create a placeholder document record
        Document document = Document.builder()
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .originalSize(file.getSize())
                .encryptedSize(file.getSize())
                .fileHash("placeholder-hash-" + UUID.randomUUID())
                .encryptionIv("placeholder-iv")
                .storagePath("/documents/" + UUID.randomUUID())
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
                String.format("Uploaded document: %s (size: %d bytes, classification: %s)",
                        file.getOriginalFilename(), file.getSize(),
                        saved.getClassificationLevel()),
                getClientIp(request),
                request.getHeader("User-Agent")
        );

        return ResponseEntity.ok(toDTO(saved));
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

    private DocumentDTO toDTO(Document doc) {
        return new DocumentDTO(
                doc.getId().toString(),
                doc.getOriginalFilename(),
                doc.getFileHash(),
                doc.getClassificationLevel().name(),
                doc.getVersionNumber(),
                doc.getUploadedBy(),
                doc.getCreatedAt().toString(),
                doc.getOriginalSize()
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
            Long fileSize
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
