package com.civicledger.exception;

import com.civicledger.entity.User.ClassificationLevel;

import java.util.UUID;

/**
 * Exception thrown when a user attempts to access a document
 * with a classification level above their clearance.
 */
public class InsufficientClearanceException extends RuntimeException {

    private final ClassificationLevel userClearance;
    private final ClassificationLevel requiredClearance;
    private final UUID documentId;

    public InsufficientClearanceException(String message) {
        super(message);
        this.userClearance = null;
        this.requiredClearance = null;
        this.documentId = null;
    }

    public InsufficientClearanceException(ClassificationLevel userClearance,
                                          ClassificationLevel requiredClearance) {
        super(formatMessage(userClearance, requiredClearance, null));
        this.userClearance = userClearance;
        this.requiredClearance = requiredClearance;
        this.documentId = null;
    }

    public InsufficientClearanceException(ClassificationLevel userClearance,
                                          ClassificationLevel requiredClearance,
                                          UUID documentId) {
        super(formatMessage(userClearance, requiredClearance, documentId));
        this.userClearance = userClearance;
        this.requiredClearance = requiredClearance;
        this.documentId = documentId;
    }

    private static String formatMessage(ClassificationLevel userClearance,
                                        ClassificationLevel requiredClearance,
                                        UUID documentId) {
        String base = String.format("Insufficient clearance: user has %s, requires %s",
                userClearance != null ? userClearance : "NONE",
                requiredClearance != null ? requiredClearance : "UNKNOWN");
        if (documentId != null) {
            return base + " for document " + documentId;
        }
        return base;
    }

    public ClassificationLevel getUserClearance() {
        return userClearance;
    }

    public ClassificationLevel getRequiredClearance() {
        return requiredClearance;
    }

    public UUID getDocumentId() {
        return documentId;
    }
}
