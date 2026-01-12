# CivicLedger Development Checklist & Feature Roadmap

## 🚀 Phase 0: Boilerplate & Infrastructure (Immediate Priority)
- [ ] **Project Initialization**
    - [ ] Create Standard Spring Boot Directory Structure (`com.civicledger`)
    - [ ] Configure `pom.xml` (Java 17, Spring Boot 3.x, Lombok, Postgres, Security)
    - [ ] Setup `.gitignore` for Java/IntelliJ/VSCode
- [ ] **Containerization Setup**
    - [ ] Create `Dockerfile` (Multi-stage build for efficient production images)
    - [ ] Create `docker-compose.yml` (PostgreSQL + Adminer/PGAdmin for local dev)
- [ ] **Environment Configuration**
    - [ ] Setup `application.properties` (or `.yml`) with profiles (dev, prod)

## 🛡️ Phase 1: The "Vault" Core (High Importance - The "Gov" Differentiators) ✅
*These features prove you understand Federal compliance (NIST 800-53).*
- [x] **Immutable Audit Engine** (Priority #1)
    - [x] Define `AuditLog` entity (Timestamp, User, Action, Resource, IP, Status)
    - [x] Implement AOP Aspect (`@Auditable`) to intercept service calls automatically
    - [x] **Why:** "If it isn't documented, it didn't happen."
- [x] **Cryptographic Module** (Priority #2)
    - [x] Implement `EncryptionService` (AES-256-GCM for file content)
    - [x] Implement `HashingService` (SHA-256 for integrity verification)
    - [x] **Why:** Mandatory for FIPS 140-2 compliance.
- [x] **Secure Storage Layer** (Priority #3)
    - [x] File System Adapter (Local dev)
    - [ ] S3/Blob Adapter (Production)
    - [x] **Constraint:** Files are never stored raw; only encrypted bytes.

## 🔐 Phase 2: Access & Identity (Medium Priority) ✅
- [x] **RBAC Implementation**
    - [x] Define Roles: `OFFICER` (Upload/Read), `AUDITOR` (Read Logs), `ADMIN`
    - [x] Implement Spring Security Filter Chain
    - [x] JWT Token Generation & Validation
- [x] **Mock Authentication**
    - [x] Create "Dev-Only" login endpoint (bypasses complex PIV/CAC integration for portfolio demo)

## 🖥️ Phase 3: Accessibility-First Frontend (Medium Priority)
- [ ] **React Boilerplate**
    - [ ] Vite + TypeScript setup
    - [ ] Tailwind CSS + `eslint-plugin-jsx-a11y`
- [ ] **Officer Dashboard** (Upload & Search)
- [ ] **Auditor Dashboard** (ReadOnly Log Stream)

## 🤖 Phase 4: AI & Automation (Bonus - Job Specific)
- [ ] **AI Summary Agent**
    - [ ] Simple integration (OpenAI/Local LLM) to generate 1-sentence summaries of uploaded text documents.
    - [ ] **Why:** Matches the "AI-assisted" requirement in the job description.

