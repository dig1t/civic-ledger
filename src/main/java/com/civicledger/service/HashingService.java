package com.civicledger.service;

import com.civicledger.exception.CryptoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * SHA-256 hashing service for file integrity verification.
 * Used to detect tampering and ensure document integrity (NIST 800-53 SI-7).
 */
@Service
@Slf4j
public class HashingService {

    private static final String ALGORITHM = "SHA-256";
    private static final int BUFFER_SIZE = 8192;

    /**
     * Computes SHA-256 hash of byte array.
     *
     * @param data The data to hash
     * @return Hex-encoded SHA-256 hash (64 characters)
     */
    public String hash(byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(data);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Computes SHA-256 hash of a string.
     *
     * @param data The string to hash
     * @return Hex-encoded SHA-256 hash (64 characters)
     */
    public String hash(String data) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        return hash(data.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Computes SHA-256 hash of an input stream.
     * Processes data in chunks to handle large files efficiently.
     *
     * @param inputStream The input stream to hash
     * @return Hex-encoded SHA-256 hash (64 characters)
     */
    public String hash(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            byte[] hashBytes = digest.digest();
            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException("SHA-256 algorithm not available", e);
        } catch (IOException e) {
            throw new CryptoException("Failed to read input stream for hashing", e);
        }
    }

    /**
     * Verifies that data matches an expected hash.
     *
     * @param data The data to verify
     * @param expectedHash The expected SHA-256 hash (hex-encoded)
     * @return true if the hash matches, false otherwise
     */
    public boolean verify(byte[] data, String expectedHash) {
        if (data == null || expectedHash == null) {
            return false;
        }

        String actualHash = hash(data);
        return constantTimeEquals(actualHash, expectedHash.toLowerCase());
    }

    /**
     * Verifies that an input stream matches an expected hash.
     *
     * @param inputStream The input stream to verify
     * @param expectedHash The expected SHA-256 hash (hex-encoded)
     * @return true if the hash matches, false otherwise
     */
    public boolean verify(InputStream inputStream, String expectedHash) {
        if (inputStream == null || expectedHash == null) {
            return false;
        }

        String actualHash = hash(inputStream);
        return constantTimeEquals(actualHash, expectedHash.toLowerCase());
    }

    /**
     * Generates a hash for multiple pieces of data concatenated together.
     * Useful for creating composite hashes (e.g., file content + metadata).
     *
     * @param parts The data parts to hash together
     * @return Hex-encoded SHA-256 hash (64 characters)
     */
    public String hashMultiple(byte[]... parts) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            for (byte[] part : parts) {
                if (part != null) {
                    digest.update(part);
                }
            }
            byte[] hashBytes = digest.digest();
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Creates a hash including a timestamp and user ID for audit purposes.
     *
     * @param data The data to hash
     * @param timestamp The timestamp
     * @param userId The user ID
     * @return Hex-encoded SHA-256 hash (64 characters)
     */
    public String hashWithContext(byte[] data, String timestamp, String userId) {
        return hashMultiple(
                data,
                timestamp.getBytes(StandardCharsets.UTF_8),
                userId.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
