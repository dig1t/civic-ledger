# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CivicLedger is a secure document management system for US Government/Defense compliance (FedRAMP/NIST 800-53). Spring Boot 3.3.0 backend with PostgreSQL database.

## Commands

```bash
# Start database
docker-compose up -d

# Build and run
mvn clean install
mvn spring-boot:run

# Tests
mvn test
mvn test -Dtest=ClassName
mvn test -Dtest=ClassName#methodName
```

## Architecture

**Security-first design:**
- Files must be encrypted before storage (AES-256-GCM)
- Calculate SHA-256 hash for all documents
- Immutable audit logs for all operations
- Use `@Aspect` for audit interceptor on service layer

**RBAC roles:** ADMINISTRATOR, OFFICER (upload/read), AUDITOR (read logs only)

**Clean separation:**
- Controllers: API layer only
- Services: Business logic
- Repositories: Data access

**Key entities to implement:**
- AuditLog: `action_type`, `user_id`, `timestamp`, `ip_address`, `resource_id` (write-once)
- Document: `file_hash`, `encryption_iv`, `version_number`, `classification_level`

See DEVELOPMENT_PLAN.md and DEV_CHECKLIST.md for implementation details.

## Documentation

**Always keep documentation up to date.** When making changes to the codebase:
- Update relevant documentation in the `docs/` directory
- Document new features, APIs, and architectural decisions
- Keep API documentation in sync with implementation changes
- Update README and guides when workflows change
