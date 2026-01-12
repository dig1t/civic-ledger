package com.civicledger.service;

import com.civicledger.exception.StorageException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * File system-based storage implementation for local development.
 * Files are organized by date (YYYY/MM/DD) with UUID-based filenames.
 *
 * IMPORTANT: Only encrypted data should be passed to this service.
 */
@Service
@Profile({"dev", "default"})
@Slf4j
public class FileSystemStorageService implements StorageService {

    private static final int BUFFER_SIZE = 8192;

    @Value("${civicledger.storage.path:./data/documents}")
    private String basePath;

    private Path rootLocation;

    @PostConstruct
    public void init() {
        rootLocation = Paths.get(basePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(rootLocation);
            log.info("Storage initialized at: {}", rootLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage location: " + rootLocation, e);
        }
    }

    @Override
    public String store(byte[] data, String filename) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }

        String storagePath = generateStoragePath(filename);
        Path targetPath = resolveStoragePath(storagePath);

        try {
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, data);
            log.debug("Stored {} bytes to: {}", data.length, storagePath);
            return storagePath;
        } catch (IOException e) {
            throw new StorageException("Failed to store file: " + storagePath, e);
        }
    }

    @Override
    public String store(InputStream inputStream, String filename, long contentLength) {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }

        String storagePath = generateStoragePath(filename);
        Path targetPath = resolveStoragePath(storagePath);

        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Stored stream to: {}", storagePath);
            return storagePath;
        } catch (IOException e) {
            throw new StorageException("Failed to store file stream: " + storagePath, e);
        }
    }

    @Override
    public byte[] retrieve(String storagePath) {
        if (storagePath == null || storagePath.isEmpty()) {
            throw new IllegalArgumentException("Storage path cannot be null or empty");
        }

        Path filePath = resolveStoragePath(storagePath);

        if (!Files.exists(filePath)) {
            throw new StorageException("File not found: " + storagePath);
        }

        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw new StorageException("Failed to read file: " + storagePath, e);
        }
    }

    @Override
    public InputStream retrieveAsStream(String storagePath) {
        if (storagePath == null || storagePath.isEmpty()) {
            throw new IllegalArgumentException("Storage path cannot be null or empty");
        }

        Path filePath = resolveStoragePath(storagePath);

        if (!Files.exists(filePath)) {
            throw new StorageException("File not found: " + storagePath);
        }

        try {
            return new BufferedInputStream(new FileInputStream(filePath.toFile()), BUFFER_SIZE);
        } catch (FileNotFoundException e) {
            throw new StorageException("File not found: " + storagePath, e);
        }
    }

    @Override
    public boolean delete(String storagePath) {
        if (storagePath == null || storagePath.isEmpty()) {
            return false;
        }

        Path filePath = resolveStoragePath(storagePath);

        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.debug("Deleted file: {}", storagePath);
                cleanEmptyDirectories(filePath.getParent());
            }
            return deleted;
        } catch (IOException e) {
            log.error("Failed to delete file: {}", storagePath, e);
            return false;
        }
    }

    @Override
    public boolean exists(String storagePath) {
        if (storagePath == null || storagePath.isEmpty()) {
            return false;
        }
        Path filePath = resolveStoragePath(storagePath);
        return Files.exists(filePath);
    }

    @Override
    public long getSize(String storagePath) {
        if (storagePath == null || storagePath.isEmpty()) {
            return -1;
        }

        Path filePath = resolveStoragePath(storagePath);

        try {
            if (Files.exists(filePath)) {
                return Files.size(filePath);
            }
            return -1;
        } catch (IOException e) {
            log.error("Failed to get file size: {}", storagePath, e);
            return -1;
        }
    }

    /**
     * Generates a unique storage path organized by date.
     * Format: YYYY/MM/DD/UUID.enc
     */
    private String generateStoragePath(String originalFilename) {
        LocalDate now = LocalDate.now();
        String datePath = now.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uniqueId = UUID.randomUUID().toString();
        return datePath + "/" + uniqueId + ".enc";
    }

    /**
     * Resolves a storage path to an absolute filesystem path.
     * Prevents path traversal attacks.
     */
    private Path resolveStoragePath(String storagePath) {
        Path resolved = rootLocation.resolve(storagePath).normalize();

        // Prevent path traversal
        if (!resolved.startsWith(rootLocation)) {
            throw new StorageException("Invalid storage path - possible path traversal attack: " + storagePath);
        }

        return resolved;
    }

    /**
     * Cleans up empty parent directories after file deletion.
     */
    private void cleanEmptyDirectories(Path directory) {
        try {
            while (directory != null && !directory.equals(rootLocation)) {
                if (Files.isDirectory(directory) && isDirectoryEmpty(directory)) {
                    Files.delete(directory);
                    directory = directory.getParent();
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            log.warn("Failed to clean empty directories", e);
        }
    }

    private boolean isDirectoryEmpty(Path directory) throws IOException {
        try (var entries = Files.list(directory)) {
            return entries.findFirst().isEmpty();
        }
    }
}
