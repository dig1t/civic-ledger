# NIST 800-53 Control Mapping

This document maps CivicLedger features to NIST 800-53 security controls for FedRAMP compliance.

## Access Control (AC)

| Control | Title | Implementation |
|---------|-------|----------------|
| AC-2 | Account Management | User entity with roles, clearance levels, active/locked status. UserService provides create, update, deactivate, unlock operations. |
| AC-3 | Access Enforcement | Spring Security `@PreAuthorize` and role-based access in SecurityConfig. Three roles: ADMINISTRATOR, OFFICER, AUDITOR. |
| AC-5 | Separation of Duties | Distinct roles with non-overlapping permissions. AUDITOR can only read logs, OFFICER can only manage documents. |
| AC-6 | Least Privilege | Role-based endpoint restrictions. Users only have access required for their function. |
| AC-7 | Unsuccessful Logon Attempts | User entity tracks `failedLoginAttempts`, `lastFailedLogin`. Automatic account lockout after threshold. |
| AC-17 | Remote Access | JWT-based stateless authentication. Token expiration enforced. |

## Audit and Accountability (AU)

| Control | Title | Implementation |
|---------|-------|----------------|
| AU-2 | Audit Events | All security-relevant events logged: login attempts, document operations, user management, configuration changes. |
| AU-3 | Content of Audit Records | AuditLog entity captures: timestamp, user_id, action_type, ip_address, resource_type, resource_id, status, details, user_agent. |
| AU-6 | Audit Review, Analysis, and Reporting | AuditLogController provides filtered queries by action type, user, date range. |
| AU-7 | Audit Reduction and Report Generation | Paginated API with filtering supports audit reduction. |
| AU-9 | Protection of Audit Information | AuditLog entity designed as write-once (no update/delete operations exposed). |
| AU-11 | Audit Record Retention | Database-level retention. Records preserved indefinitely by default. |
| AU-12 | Audit Generation | @Auditable AOP aspect automatically captures audit events on annotated service methods. |

## Identification and Authentication (IA)

| Control | Title | Implementation |
|---------|-------|----------------|
| IA-2 | Identification and Authentication | JWT-based authentication. Email/password with optional MFA code. |
| IA-4 | Identifier Management | UUID-based user identifiers. Email uniqueness enforced. |
| IA-5 | Authenticator Management | BCrypt password hashing (cost factor 12). Password field never exposed in responses. |
| IA-8 | Identification and Authentication (Non-Org Users) | Keycloak integration for federated identity (production mode). |

## System and Communications Protection (SC)

| Control | Title | Implementation |
|---------|-------|----------------|
| SC-8 | Transmission Confidentiality | TLS 1.3 required for all communications (infrastructure level). |
| SC-12 | Cryptographic Key Establishment | AES-256 master key from environment variable. Key generation uses SecureRandom. |
| SC-13 | Cryptographic Protection | AES-256-GCM for encryption (FIPS 140-2 approved). SHA-256 for integrity verification. |
| SC-28 | Protection of Information at Rest | All documents encrypted with AES-256-GCM before storage. EncryptionService handles all cryptographic operations. |

## System and Information Integrity (SI)

| Control | Title | Implementation |
|---------|-------|----------------|
| SI-7 | Software, Firmware, and Information Integrity | SHA-256 hash computed on upload, verified on download. HashingService.verify() uses constant-time comparison. |
| SI-10 | Information Input Validation | Spring Validation annotations on DTOs. Input sanitization on all API endpoints. |

## Configuration Management (CM)

| Control | Title | Implementation |
|---------|-------|----------------|
| CM-2 | Baseline Configuration | Docker containerization with defined base images. |
| CM-6 | Configuration Settings | Spring profiles (dev, prod) with environment-specific settings. |
| CM-8 | Information System Component Inventory | Maven dependency management. Bill of materials in pom.xml. |

## Implementation Files

| Control Family | Primary Implementation Files |
|----------------|------------------------------|
| AC (Access Control) | SecurityConfig.java, User.java, UserService.java |
| AU (Audit) | AuditLog.java, AuditAspect.java, AuditService.java, AuditLogController.java |
| IA (Identification) | AuthController.java, JwtService.java, JwtAuthenticationFilter.java |
| SC (System Protection) | EncryptionService.java, HashingService.java |
| SI (System Integrity) | HashingService.java, DocumentController.java |

## Compliance Status

| Category | Controls Addressed | Status |
|----------|-------------------|--------|
| Access Control (AC) | 6 | Implemented |
| Audit and Accountability (AU) | 7 | Implemented |
| Identification and Authentication (IA) | 4 | Implemented |
| System and Communications Protection (SC) | 4 | Implemented |
| System and Information Integrity (SI) | 2 | Implemented |
| Configuration Management (CM) | 3 | Partial |

**Total Controls Mapped: 26**

## Notes

1. This mapping covers controls relevant to the application layer. Infrastructure controls (physical security, network configuration, etc.) must be addressed separately.

2. Full FedRAMP compliance requires additional documentation including:
   - System Security Plan (SSP)
   - Security Assessment Report (SAR)
   - Plan of Action and Milestones (POA&M)

3. Production deployment should enable Keycloak integration for enterprise SSO and CAC/PIV card support.
