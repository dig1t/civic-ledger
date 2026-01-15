# CivicLedger

A secure document management system designed for U.S. Government and Defense compliance with FedRAMP and NIST 800-53 security controls.

## Table of Contents

- [About](#about)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Usage](#usage)
- [API Documentation](#api-documentation)
- [Security](#security)
- [Contributing](#contributing)
- [License](#license)

## About

CivicLedger provides a secure platform for managing sensitive documents with full audit trails, encryption at rest, and role-based access control. Built to meet federal compliance requirements including:

- **FedRAMP** - Federal Risk and Authorization Management Program
- **NIST 800-53** - Security and Privacy Controls
- **FIPS 140-2** - Cryptographic Module Standards
- **Section 508 / WCAG 2.1 AA** - Accessibility Standards

## Features

- AES-256-GCM encryption for documents at rest
- SHA-256 integrity verification
- Immutable audit logging
- Role-Based Access Control (RBAC)
- Classification level enforcement
- AI-powered document summarization
- Accessible user interface

## Prerequisites

- **Java 17** or higher
- **Node.js 18** or higher
- **Docker** and **Docker Compose**
- **Maven 3.8+**

## Installation

### 1. Clone the Repository

```bash
git clone https://github.com/your-org/civic-ledger.git
cd civic-ledger
```

### 2. Start Database Services

```bash
docker-compose up -d
```

This starts:
- PostgreSQL database (port 5432)
- Keycloak identity provider (port 8180)
- Adminer database UI (port 8081)

### 3. Configure Environment Variables

**Backend:**

```bash
cp .env.example .env
```

Edit `.env` and set required values:

```bash
# Required - Generate with: openssl rand -base64 32
ENCRYPTION_MASTER_KEY=your-base64-encryption-key-here

# Optional - For AI document summarization
OPENAI_API_KEY=sk-proj-your-api-key-here
```

**Frontend:**

```bash
cp frontend/.env.example frontend/.env
```

Default values work for local development.

### 4. Build and Run Backend

```bash
mvn clean install
mvn spring-boot:run
```

The API server starts at `http://localhost:8080`.

### 5. Build and Run Frontend

```bash
cd frontend
npm install
npm run dev
```

The web application starts at `http://localhost:3000`.

## Configuration

### Environment Profiles

| Profile | Purpose | Database DDL |
|---------|---------|--------------|
| `dev` (default) | Local development | Auto-update |
| `prod` | Production deployment | Validate only |

### Role-Based Access Control

| Role | Permissions |
|------|-------------|
| ADMINISTRATOR | Full system access |
| OFFICER | Upload, view, download documents |
| AUDITOR | View audit logs only |

### Classification Levels

Documents support the following classification levels:
- UNCLASSIFIED
- CONFIDENTIAL
- SECRET
- TOP_SECRET

Users can only access documents at or below their clearance level.

## Usage

### Default Development Credentials

For local development with mock authentication:

| User | Role | Email |
|------|------|-------|
| Admin | ADMINISTRATOR | admin@civicledger.gov |

### Common Operations

**Upload a Document:**
```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -H "Authorization: Bearer <token>" \
  -F "file=@document.pdf" \
  -F "classificationLevel=UNCLASSIFIED"
```

**List Documents:**
```bash
curl http://localhost:8080/api/documents \
  -H "Authorization: Bearer <token>"
```

**Download a Document:**
```bash
curl http://localhost:8080/api/documents/{id}/download \
  -H "Authorization: Bearer <token>" \
  -o document.pdf
```

## API Documentation

Interactive API documentation is available at:
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI Spec:** `http://localhost:8080/v3/api-docs`

## Security

### Encryption

- All documents are encrypted using AES-256-GCM before storage
- Encryption keys are derived from the `ENCRYPTION_MASTER_KEY` environment variable
- SHA-256 hashes verify document integrity on every download

### Audit Logging

All operations are logged to an immutable audit trail including:
- User identification
- Timestamp
- Action type
- IP address
- Resource affected
- Success/failure status

### Reporting Security Issues

To report a security vulnerability, please contact the security team directly. Do not open a public issue.

See [SECURITY.md](SECURITY.md) for the full security policy.

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting pull requests.

### Development Commands

```bash
# Run backend tests
mvn test

# Run specific test class
mvn test -Dtest=ClassName

# Run frontend linting
cd frontend && npm run lint

# Build for production
mvn clean package -DskipTests
cd frontend && npm run build
```

## License

This project is licensed under the [MIT License](LICENSE).

---

## Additional Resources

- [Development Plan](docs/DEVELOPMENT_PLAN.md)
- [Architecture Documentation](docs/ARCHITECTURE.md)
- [NIST 800-53 Controls Mapping](docs/NIST_CONTROLS.md)
- [API Documentation](docs/API.md)

## Support

For questions or issues, please open a GitHub issue or contact the development team.
