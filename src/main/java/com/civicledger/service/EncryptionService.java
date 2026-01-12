package com.civicledger.service;

import com.civicledger.exception.CryptoException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption service for FIPS 140-2 compliant file encryption.
 * Uses authenticated encryption to provide both confidentiality and integrity.
 */
@Service
@Slf4j
public class EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits recommended for GCM
    private static final int GCM_TAG_LENGTH = 128; // 128-bit authentication tag
    private static final int AES_KEY_SIZE = 256;

    private final SecretKey masterKey;
    private final SecureRandom secureRandom;

    public EncryptionService(@Value("${civicledger.encryption.master-key:}") String masterKeyBase64) {
        this.secureRandom = new SecureRandom();

        if (masterKeyBase64 == null || masterKeyBase64.isEmpty()) {
            log.warn("No master encryption key configured. Generating ephemeral key for development only.");
            this.masterKey = generateKey();
            log.warn("Generated ephemeral master key (Base64): {}",
                    Base64.getEncoder().encodeToString(masterKey.getEncoded()));
            log.warn("Set 'civicledger.encryption.master-key' in production!");
        } else {
            byte[] keyBytes = Base64.getDecoder().decode(masterKeyBase64);
            if (keyBytes.length != 32) {
                throw new IllegalArgumentException("Master key must be 256 bits (32 bytes)");
            }
            this.masterKey = new SecretKeySpec(keyBytes, ALGORITHM);
            log.info("Master encryption key loaded successfully");
        }
    }

    /**
     * Encrypts data using AES-256-GCM.
     * Returns the encrypted data with the IV prepended.
     *
     * @param plaintext The data to encrypt
     * @return EncryptionResult containing ciphertext and IV
     */
    public EncryptionResult encrypt(byte[] plaintext) {
        if (plaintext == null || plaintext.length == 0) {
            throw new IllegalArgumentException("Plaintext cannot be null or empty");
        }

        try {
            byte[] iv = generateIV();
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, spec);

            byte[] ciphertext = cipher.doFinal(plaintext);

            return new EncryptionResult(ciphertext, iv);

        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new CryptoException("Failed to encrypt data", e);
        }
    }

    /**
     * Decrypts data encrypted with AES-256-GCM.
     *
     * @param ciphertext The encrypted data
     * @param iv The initialization vector used during encryption
     * @return The decrypted plaintext
     */
    public byte[] decrypt(byte[] ciphertext, byte[] iv) {
        if (ciphertext == null || ciphertext.length == 0) {
            throw new IllegalArgumentException("Ciphertext cannot be null or empty");
        }
        if (iv == null || iv.length != GCM_IV_LENGTH) {
            throw new IllegalArgumentException("IV must be " + GCM_IV_LENGTH + " bytes");
        }

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, spec);

            return cipher.doFinal(ciphertext);

        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new CryptoException("Failed to decrypt data - possible tampering detected", e);
        }
    }

    /**
     * Encrypts data and returns combined IV + ciphertext for easy storage.
     */
    public byte[] encryptCombined(byte[] plaintext) {
        EncryptionResult result = encrypt(plaintext);
        byte[] combined = new byte[GCM_IV_LENGTH + result.ciphertext().length];
        System.arraycopy(result.iv(), 0, combined, 0, GCM_IV_LENGTH);
        System.arraycopy(result.ciphertext(), 0, combined, GCM_IV_LENGTH, result.ciphertext().length);
        return combined;
    }

    /**
     * Decrypts combined IV + ciphertext data.
     */
    public byte[] decryptCombined(byte[] combined) {
        if (combined == null || combined.length <= GCM_IV_LENGTH) {
            throw new IllegalArgumentException("Invalid combined ciphertext");
        }

        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
        System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

        return decrypt(ciphertext, iv);
    }

    /**
     * Generates a new AES-256 key. Useful for key rotation or per-document keys.
     */
    public SecretKey generateKey() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance(ALGORITHM);
            keyGen.init(AES_KEY_SIZE, secureRandom);
            return keyGen.generateKey();
        } catch (Exception e) {
            throw new CryptoException("Failed to generate encryption key", e);
        }
    }

    /**
     * Generates a cryptographically secure random IV.
     */
    public byte[] generateIV() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        return iv;
    }

    /**
     * Encodes bytes to Base64 string for storage.
     */
    public String encodeToBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * Decodes Base64 string back to bytes.
     */
    public byte[] decodeFromBase64(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    /**
     * Result of an encryption operation.
     */
    public record EncryptionResult(byte[] ciphertext, byte[] iv) {

        public String ciphertextBase64() {
            return Base64.getEncoder().encodeToString(ciphertext);
        }

        public String ivBase64() {
            return Base64.getEncoder().encodeToString(iv);
        }
    }
}
