# CivicLedger Architecture

## Overview

CivicLedger is a secure document management system designed for US Government/Defense compliance (FedRAMP/NIST 800-53).

## Technology Stack

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 3.3.0, Java 17 |
| Database | PostgreSQL (Neon.tech) |
| Frontend | Next.js 14+, TypeScript, TailwindCSS |
| Authentication | JWT (Mock mode) / Keycloak (Production) |
| Encryption | AES-256-GCM |
| Hashing | SHA-256 |

## Security Architecture

### Authentication Flow
```
User -> Login -> JWT Token -> Authorization Header -> Protected Resources
```

### Document Security Flow
```
Upload:
  File Bytes -> SHA-256 Hash -> AES-256-GCM Encrypt -> Store Encrypted -> Save Metadata

Download:
  Retrieve Encrypted -> Decrypt -> Verify Hash -> Stream to User
```

### Role-Based Access Control (RBAC)

| Role | Permissions |
|------|-------------|
| ADMINISTRATOR | Full access to all resources |
| OFFICER | Upload, view, download, delete documents |
| AUDITOR | Read-only access to audit logs |

## Package Structure

```
com.civicledger/
├── config/           # Spring configuration classes
│   ├── SecurityConfig.java
│   └── OpenApiConfig.java
├── controller/       # REST API endpoints
│   ├── AuthController.java
│   ├── DocumentController.java
│   ├── AuditLogController.java
│   └── HealthController.java
├── entity/           # JPA entities
│   ├── User.java
│   ├── Document.java
│   └── AuditLog.java
├── repository/       # Data access layer
├── service/          # Business logic
│   ├── EncryptionService.java
│   ├── HashingService.java
│   ├── StorageService.java
│   └── AuditService.java
├── security/         # Security components
│   ├── JwtService.java
│   └── JwtAuthenticationFilter.java
└── audit/            # Audit AOP components
    ├── Auditable.java
    └── AuditAspect.java
```

## Key Components

### EncryptionService
- Algorithm: AES-256-GCM (FIPS 140-2 compliant)
- 12-byte IV, 128-bit authentication tag
- Master key from environment or auto-generated for dev

### HashingService
- Algorithm: SHA-256
- Used for file integrity verification
- Constant-time comparison to prevent timing attacks

### StorageService
- Interface for pluggable storage backends
- FileSystemStorageService for local development
- Files stored as: `YYYY/MM/DD/UUID.enc`

### AuditAspect
- AOP-based automatic audit logging
- Intercepts methods annotated with `@Auditable`
- Captures action type, user, IP, status, details

## Database Schema

### users
- id, email, password_hash, full_name, roles, clearance_level, etc.

### documents
- id, original_filename, file_hash, encryption_iv, storage_path, etc.

### audit_logs
- id, user_id, action_type, resource_type, resource_id, timestamp, ip_address, etc.
- Write-once (immutable)

## Compliance Features

| NIST Control | Implementation |
|--------------|----------------|
| AC-2 (Account Management) | RBAC with roles, user lifecycle management |
| AU-2 (Audit Events) | Immutable audit logs for all actions |
| SC-8 (Transmission Confidentiality) | TLS 1.3 required |
| SC-28 (Protection at Rest) | AES-256-GCM encryption |
| SI-7 (Software Integrity) | SHA-256 hash verification |
