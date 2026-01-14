# Compliance Fix TODO

Tracking fixes for compliance gaps identified against CLAUDE.md, DEVELOPMENT_PLAN.md, and DEV_CHECKLIST.md.

## Critical Fixes

- [x] **1. Wire EncryptionService into document upload**
  - File: `DocumentController.java`
  - Issue: Uses placeholder hash/iv instead of actual encryption
  - Fix: Call HashingService for SHA-256, EncryptionService for AES-256-GCM
  - DONE: Upload now hashes with SHA-256, encrypts with AES-256-GCM, stores encrypted bytes
  - DONE: Download now retrieves, decrypts, verifies hash integrity

- [x] **2. Add Kubernetes health probe endpoints**
  - File: `HealthController.java`
  - Issue: Only `/api/health` exists
  - Fix: Add `/health/ready` and `/health/live` endpoints
  - DONE: Added `/health/live` (liveness) and `/health/ready` (readiness with DB check)
  - DONE: Updated SecurityConfig to allow public access to `/health/**`

- [x] **3. Add OpenAPI/Swagger documentation**
  - File: `pom.xml`, new config class
  - Issue: Not configured
  - Fix: Add springdoc-openapi dependency and configuration
  - DONE: Added springdoc-openapi-starter-webmvc-ui 2.5.0 dependency
  - DONE: Created OpenApiConfig.java with JWT auth scheme
  - DONE: Updated SecurityConfig to allow /swagger-ui/** and /v3/api-docs/**
  - Access at: /swagger-ui.html

- [x] **4. Create docs/ directory**
  - Issue: Directory doesn't exist
  - Fix: Create docs/ with initial documentation
  - DONE: Created docs/API.md with endpoint documentation
  - DONE: Created docs/ARCHITECTURE.md with system overview

- [x] **5. Create NIST_CONTROLS.md**
  - Issue: File doesn't exist
  - Fix: Map features to NIST 800-53 controls
  - DONE: Created docs/NIST_CONTROLS.md mapping 26 controls across 6 families

## Progress Log

| Date | Item | Status |
|------|------|--------|
| 2026-01-13 | Wire EncryptionService into document upload | Complete |
| 2026-01-13 | Add Kubernetes health probe endpoints | Complete |
| 2026-01-13 | Add OpenAPI/Swagger documentation | Complete |
| 2026-01-13 | Create docs/ directory | Complete |
| 2026-01-13 | Create NIST_CONTROLS.md | Complete |
| 2026-01-13 | ALL COMPLIANCE FIXES COMPLETE | Done |
