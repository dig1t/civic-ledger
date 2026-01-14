Development Plan: CivicLedger Secure Document Management
Target Environment: US Government / Defense (FedRAMP/NIST 800-53 focus)
Tech Stack:
- Backend: Spring Boot 3.x (Java 17+)
- Database: PostgreSQL (Neon.tech)
- Frontend: Next.js 14+ (App Router, TypeScript, TailwindCSS)
- Deployment: Docker (Render for Backend, Vercel for Frontend)

1. Project Vision & Compliance Goals
The goal is a "Chain of Custody" document system. Every action must be non-repudiable.

Security: AES-256 at-rest, TLS 1.3 in-transit.

Integrity: SHA-256 file hashing to prevent tampering.

Auditability: Immutable audit logs (Write-Once-Read-Many logic).

Accessibility: Section 508 / WCAG 2.1 AA Compliance.

2. Phase 1: Core Security & Persistence Layer
[ ] Database Schema:

Users: Support for RBAC (Roles: Administrator, Officer, Auditor).

Documents: Store file_hash, encryption_iv, version_number, and classification_level.

AuditLogs: Fields for action_type, user_id, timestamp, ip_address, and resource_id.

[ ] Encryption Service:

Implement an IEncryptionService using AES-256-GCM.

Files must be encrypted before writing to the StorageService.

[ ] Audit Interceptor:

(Spring): Use @Aspect to intercept all Service-layer calls.

3. Phase 2: Secure API Development
[ ] Authentication:

Implement JWT with a short TTL.

Add a "Mock MFA" header check to simulate PIV/CAC card requirements.

[ ] Document Handlers:

Upload: Calculate SHA-256 hash -> Encrypt -> Store Metadata -> Write to Disk/S3.

Download: Verify User Permission -> Retrieve -> Decrypt -> Verify Hash -> Stream to User.

[ ] Role-Based Access Control (RBAC):

Strict decorators (e.g., @PreAuthorize("hasRole('AUDITOR')")) for the Audit log endpoints.

4. Phase 3: Section 508 Compliant Frontend (Next.js)
[ ] Setup:

Next.js 14+ with App Router and TypeScript.

TailwindCSS with high-contrast configuration for accessibility.

Consider USWDS (U.S. Web Design System) design tokens where applicable.

[ ] Accessibility Requirements:

Full keyboard navigation (Focus rings visible).

aria-live regions for file upload progress.

Semantic HTML (main, nav, section tags).

Server Components for improved performance and SEO.

[ ] Views:

Officer Dashboard: Document upload and search.

Auditor View: Read-only chronological stream of system events.

5. Phase 4: Hardening & DevSecOps (The "Gov" Edge)
[ ] Dockerization:

Use Distroless or Alpine base images to reduce vulnerabilities.

Run as a non-root user within the container.

[ ] Documentation:

Generate Swagger/OpenAPI docs.

Create a NIST_CONTROLS.md mapping features to NIST 800-53 controls (e.g., AC-2 for Account Management, AU-2 for Audit Events).

[ ] Health Checks:

Implement /health/ready and /health/live for Kubernetes readiness probes.

6. Prompting Instructions for Cursor/Opus
When implementing this plan, Claude/Opus should:

Prioritize Defensive Coding: Always validate inputs, check for nulls, and wrap cryptographic operations in try-catch blocks.

Verbose Logging: Use structured logging (JSON) so logs are ready for an ELK stack.

Clean Architecture: Keep business logic (Services) separate from delivery (Controllers/API).

