package com.civicledger.service;

import java.io.InputStream;

/**
 * Interface for secure file storage operations.
 * Implementations must ensure files are never stored in plaintext.
 */
public interface StorageService {

    /**
     * Stores encrypted data and returns the storage location identifier.
     *
     * @param data The encrypted data to store
     * @param filename The original filename (for reference only)
     * @return The storage path/key used to retrieve the file
     */
    String store(byte[] data, String filename);

    /**
     * Stores encrypted data from an input stream.
     *
     * @param inputStream The encrypted data stream
     * @param filename The original filename (for reference only)
     * @param contentLength The expected content length (-1 if unknown)
     * @return The storage path/key used to retrieve the file
     */
    String store(InputStream inputStream, String filename, long contentLength);

    /**
     * Retrieves encrypted data from storage.
     *
     * @param storagePath The storage path/key
     * @return The encrypted data as byte array
     */
    byte[] retrieve(String storagePath);

    /**
     * Retrieves encrypted data as an input stream (for large files).
     *
     * @param storagePath The storage path/key
     * @return The encrypted data as an input stream
     */
    InputStream retrieveAsStream(String storagePath);

    /**
     * Deletes a file from storage.
     *
     * @param storagePath The storage path/key
     * @return true if deletion was successful
     */
    boolean delete(String storagePath);

    /**
     * Checks if a file exists in storage.
     *
     * @param storagePath The storage path/key
     * @return true if the file exists
     */
    boolean exists(String storagePath);

    /**
     * Gets the size of a stored file in bytes.
     *
     * @param storagePath The storage path/key
     * @return The file size in bytes, or -1 if not found
     */
    long getSize(String storagePath);
}
