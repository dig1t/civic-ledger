# TODO: Secure Sensitive Configuration

**Task:** Migrate hardcoded secrets in development configuration to environment variables
**Priority:** High (Security)
**Status:** Not Started
**Created:** 2026-01-15

---

## Overview

The `application-dev.properties` file contains hardcoded sensitive values that should be externalized to environment variables. This aligns with security best practices and the existing pattern used in `application-prod.properties`.

### Current State Analysis

**Sensitive values currently hardcoded in `application-dev.properties`:**

| Line | Property | Current Value | Risk Level |
|------|----------|---------------|------------|
| 5 | `spring.datasource.username` | `admin` | Medium |
| 6 | `spring.datasource.password` | `securepassword123!` | High |
| 20 | `civicledger.jwt.secret` | `devSecretKeyForLocal...` (64 chars) | High |
| 26 | `civicledger.auth.mock-mfa-code` | `123456` | Medium |

**Already externalized (good):**
- Line 41: `civicledger.encryption.master-key=${ENCRYPTION_MASTER_KEY:}`
- Line 46: `civicledger.ai.openai.api-key=${OPENAI_API_KEY:}`

**Non-sensitive configuration to keep in properties file:**
- Database URL (localhost reference is acceptable for dev)
- JPA/Hibernate settings (ddl-auto, show-sql, dialect)
- Authentication mode flag
- JWT expiration and issuer settings
- MFA requirement flag
- Logging levels
- Storage path
- AI feature flags and model settings

---

## Task Breakdown

### Phase 1: Pre-Implementation Verification

- [ ] **1.1** Verify `.env` is in `.gitignore`
  - Status: VERIFIED - `.gitignore` line 40 contains `.env`
  - No action required

- [ ] **1.2** Review `docker-compose.yml` for database credentials
  - Check if Docker Compose uses environment variables or hardcoded values
  - Ensure consistency between Docker and Spring configuration

### Phase 2: Create Environment Variable Template

- [ ] **2.1** Create `.env.example` file in project root
  - Location: `/Users/dig1t/Git/civic-ledger/.env.example`
  - Include all required environment variables with placeholder values
  - Add comments explaining each variable
  - Group variables by category (Database, Auth, Encryption, AI)

**Proposed `.env.example` content:**
```bash
# ===========================================
# CivicLedger Environment Variables
# ===========================================
# Copy this file to .env and fill in actual values
# NEVER commit .env to version control

# --- Database Configuration ---
DATABASE_URL=jdbc:postgresql://localhost:5432/civicledger
DATABASE_USERNAME=your_db_username
DATABASE_PASSWORD=your_db_password

# --- JWT Authentication (Dev Mock Mode) ---
# Generate with: openssl rand -base64 64
JWT_SECRET=your_jwt_secret_min_64_characters

# --- MFA Configuration (Dev Mock Mode) ---
MOCK_MFA_CODE=your_6_digit_code

# --- Encryption ---
# Generate with: openssl rand -base64 32
ENCRYPTION_MASTER_KEY=your_encryption_key

# --- AI/OpenAI Integration ---
OPENAI_API_KEY=your_openai_api_key

# --- Production Only (Keycloak) ---
# KEYCLOAK_ISSUER_URI=https://your-keycloak-server/realms/civicledger
# KEYCLOAK_JWK_URI=https://your-keycloak-server/realms/civicledger/protocol/openid-connect/certs
```

### Phase 3: Update Development Configuration

- [ ] **3.1** Update `application-dev.properties` datasource credentials
  - Change line 5: `spring.datasource.username=${DATABASE_USERNAME:admin}`
  - Change line 6: `spring.datasource.password=${DATABASE_PASSWORD:}`
  - Note: Keeping default username for convenience; password requires explicit setting

- [ ] **3.2** Update JWT secret configuration
  - Change line 20: `civicledger.jwt.secret=${JWT_SECRET:}`
  - Remove the hardcoded development key

- [ ] **3.3** Update mock MFA code
  - Change line 26: `civicledger.auth.mock-mfa-code=${MOCK_MFA_CODE:}`
  - Remove the hardcoded value

### Phase 4: Documentation and Developer Experience

- [ ] **4.1** Create local `.env` file for development (do not commit)
  - Copy from `.env.example`
  - Fill in development values
  - Verify application starts correctly

- [ ] **4.2** Update project README or setup documentation
  - Add instructions for copying `.env.example` to `.env`
  - Document how to generate secure values (openssl commands)
  - Note that `.env` is gitignored

- [ ] **4.3** Update `docker-compose.yml` if needed
  - Ensure Docker services can read from `.env` file
  - Or use `env_file` directive in compose

### Phase 5: Validation and Testing

- [ ] **5.1** Test application startup without `.env` file
  - Verify appropriate error messages for missing required variables
  - Confirm non-sensitive defaults still work

- [ ] **5.2** Test application startup with `.env` file
  - Verify all environment variables are loaded correctly
  - Test database connectivity
  - Test JWT authentication
  - Test MFA flow (if applicable)

- [ ] **5.3** Verify no secrets in version control
  - Run `git grep` for known secret patterns
  - Check git history for any previously committed secrets

---

## Files to Modify

| File | Action | Description |
|------|--------|-------------|
| `.env.example` | CREATE | Environment variable template with placeholders |
| `src/main/resources/application-dev.properties` | MODIFY | Replace hardcoded values with `${VAR:}` syntax |
| `docs/` or `README.md` | MODIFY | Add setup instructions for environment variables |

---

## Acceptance Criteria

1. No hardcoded sensitive values remain in `application-dev.properties`
2. `.env.example` exists with all required variables documented
3. `.env` remains in `.gitignore` (already verified)
4. Application starts successfully with properly configured `.env` file
5. Clear error messages appear when required environment variables are missing
6. Developer documentation updated with setup instructions

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Developers forget to create `.env` | Medium | Low | Clear error messages and documentation |
| Inconsistent env var naming | Low | Medium | Follow existing prod naming conventions |
| Docker Compose sync issues | Medium | Medium | Update compose file to use same env vars |

---

## Notes

- The `application-prod.properties` file already follows best practices with environment variables
- Consider using Spring Boot's `@ConfigurationProperties` for type-safe configuration validation
- For production, recommend using a secrets manager (AWS Secrets Manager, HashiCorp Vault, etc.)
- The FedRAMP/NIST 800-53 compliance requirements make this security hardening essential
