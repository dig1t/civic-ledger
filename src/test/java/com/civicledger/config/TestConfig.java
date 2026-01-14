package com.civicledger.config;

import com.civicledger.service.StorageService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test configuration that provides mock beans for testing.
 */
@Configuration
@Profile("test")
public class TestConfig {

    /**
     * In-memory storage service for tests.
     */
    @Bean
    public StorageService storageService() {
        return new InMemoryStorageService();
    }

    /**
     * Simple in-memory implementation of StorageService for tests.
     */
    static class InMemoryStorageService implements StorageService {
        private final Map<String, byte[]> storage = new ConcurrentHashMap<>();

        @Override
        public String store(byte[] data, String filename) {
            String path = UUID.randomUUID().toString() + ".enc";
            storage.put(path, data);
            return path;
        }

        @Override
        public String store(InputStream inputStream, String filename, long contentLength) {
            try {
                byte[] data = inputStream.readAllBytes();
                return store(data, filename);
            } catch (Exception e) {
                throw new RuntimeException("Failed to read input stream", e);
            }
        }

        @Override
        public byte[] retrieve(String storagePath) {
            byte[] data = storage.get(storagePath);
            if (data == null) {
                throw new RuntimeException("File not found: " + storagePath);
            }
            return data;
        }

        @Override
        public InputStream retrieveAsStream(String storagePath) {
            return new ByteArrayInputStream(retrieve(storagePath));
        }

        @Override
        public boolean delete(String storagePath) {
            return storage.remove(storagePath) != null;
        }

        @Override
        public boolean exists(String storagePath) {
            return storage.containsKey(storagePath);
        }

        @Override
        public long getSize(String storagePath) {
            byte[] data = storage.get(storagePath);
            return data != null ? data.length : -1;
        }
    }
}
