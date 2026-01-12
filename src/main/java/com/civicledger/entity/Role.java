package com.civicledger.entity;

/**
 * RBAC roles for CivicLedger aligned with NIST 800-53 AC-2.
 */
public enum Role {

    /**
     * Full system access - user management, configuration, all document operations.
     */
    ADMINISTRATOR("System Administrator with full access"),

    /**
     * Can upload, read, and manage documents. Primary operational role.
     */
    OFFICER("Document Officer - can upload and read documents"),

    /**
     * Read-only access to audit logs. Cannot access documents directly.
     */
    AUDITOR("Auditor - read-only access to audit logs");

    private final String description;

    Role(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Returns the Spring Security role name (prefixed with ROLE_).
     */
    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
