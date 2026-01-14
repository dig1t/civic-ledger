package com.civicledger.security;

import com.civicledger.entity.Document;
import com.civicledger.entity.User;
import com.civicledger.entity.User.ClassificationLevel;
import com.civicledger.exception.InsufficientClearanceException;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for validating user clearance against document classification levels.
 * Implements NIST 800-53 AC-3 (Access Enforcement) controls.
 *
 * <p>Classification hierarchy (lowest to highest):
 * UNCLASSIFIED → CUI → CONFIDENTIAL → SECRET → TOP_SECRET
 *
 * <p>A user can access a document if their clearance level is greater than
 * or equal to the document's classification level.
 */
@Service
public class ClearanceValidator {

    /**
     * Check if a user has sufficient clearance to access a document.
     *
     * @param user the user attempting access
     * @param document the document being accessed
     * @return true if access is permitted, false otherwise
     */
    public boolean hasAccess(User user, Document document) {
        if (user == null || document == null) {
            return false;
        }

        ClassificationLevel userClearance = user.getClearanceLevel();
        ClassificationLevel docClassification = document.getClassificationLevel();

        return hasAccess(userClearance, docClassification);
    }

    /**
     * Check if a clearance level permits access to a classification level.
     *
     * @param userClearance the user's clearance level (null treated as UNCLASSIFIED)
     * @param docClassification the document's classification level
     * @return true if access is permitted, false otherwise
     */
    public boolean hasAccess(ClassificationLevel userClearance, ClassificationLevel docClassification) {
        if (docClassification == null) {
            return true; // Unclassified documents are accessible to all
        }

        // Null clearance is treated as UNCLASSIFIED
        ClassificationLevel effectiveClearance = userClearance != null
                ? userClearance
                : ClassificationLevel.UNCLASSIFIED;

        // User can access if their clearance ordinal >= document classification ordinal
        return effectiveClearance.ordinal() >= docClassification.ordinal();
    }

    /**
     * Validate access and throw exception if denied.
     *
     * @param user the user attempting access
     * @param document the document being accessed
     * @throws InsufficientClearanceException if access is denied
     */
    public void validateAccess(User user, Document document) {
        if (!hasAccess(user, document)) {
            ClassificationLevel userClearance = user != null ? user.getClearanceLevel() : null;
            ClassificationLevel docClassification = document != null ? document.getClassificationLevel() : null;

            throw new InsufficientClearanceException(
                    userClearance,
                    docClassification,
                    document != null ? document.getId() : null
            );
        }
    }

    /**
     * Get all classification levels a user with the given clearance can access.
     *
     * @param userClearance the user's clearance level (null treated as UNCLASSIFIED)
     * @return list of accessible classification levels
     */
    public List<ClassificationLevel> getAccessibleLevels(ClassificationLevel userClearance) {
        ClassificationLevel effectiveClearance = userClearance != null
                ? userClearance
                : ClassificationLevel.UNCLASSIFIED;

        return Arrays.stream(ClassificationLevel.values())
                .filter(level -> level.ordinal() <= effectiveClearance.ordinal())
                .collect(Collectors.toList());
    }

    /**
     * Get the maximum classification level a user can access.
     *
     * @param user the user
     * @return the maximum accessible classification level
     */
    public ClassificationLevel getMaxAccessibleLevel(User user) {
        if (user == null || user.getClearanceLevel() == null) {
            return ClassificationLevel.UNCLASSIFIED;
        }
        return user.getClearanceLevel();
    }
}
