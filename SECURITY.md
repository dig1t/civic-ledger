# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.x.x   | :white_check_mark: |

## Reporting a Vulnerability

The CivicLedger team takes security vulnerabilities seriously. We appreciate your efforts to responsibly disclose your findings.

### How to Report

**Do not report security vulnerabilities through public GitHub issues.**

Instead, please report them by emailing the security team directly. You should receive a response within 48 hours. If for some reason you do not, please follow up to ensure we received your original message.

Please include the following information in your report:

- Type of vulnerability (e.g., SQL injection, XSS, authentication bypass)
- Full paths of source file(s) related to the vulnerability
- Location of the affected source code (tag/branch/commit or direct URL)
- Step-by-step instructions to reproduce the issue
- Proof-of-concept or exploit code (if possible)
- Impact of the issue, including how an attacker might exploit it

### What to Expect

- **Acknowledgment:** We will acknowledge receipt of your vulnerability report within 48 hours.
- **Communication:** We will keep you informed of the progress toward a fix and full announcement.
- **Credit:** We will credit you in the security advisory if you wish (please let us know your preference).

### Safe Harbor

We consider security research conducted consistent with this policy to be:

- Authorized concerning any applicable anti-hacking laws
- Authorized concerning any relevant anti-circumvention laws
- Exempt from restrictions in our Terms of Service that would interfere with conducting security research

We will not pursue civil action or initiate a complaint to law enforcement for accidental, good-faith violations of this policy.

## Security Controls

CivicLedger implements the following security controls:

### Encryption
- AES-256-GCM encryption for all documents at rest
- TLS 1.2+ for all data in transit
- Secure key management practices

### Authentication & Authorization
- Role-Based Access Control (RBAC)
- JWT token-based authentication
- OAuth2/OIDC support via Keycloak
- Session management with secure defaults

### Audit & Logging
- Immutable audit logs for all operations
- Tamper-evident log integrity verification
- Comprehensive event tracking

### Input Validation
- Server-side validation for all inputs
- Protection against SQL injection
- Protection against XSS attacks
- CSRF protection

### Infrastructure
- Non-root container execution
- Health checks and monitoring
- Secure default configurations

## Compliance

This system is designed to comply with:

- **NIST 800-53** - Security and Privacy Controls for Information Systems
- **FedRAMP** - Federal Risk and Authorization Management Program
- **FIPS 140-2** - Security Requirements for Cryptographic Modules
- **Section 508** - Accessibility Standards

## Security Updates

Security updates are released as needed. We recommend keeping your installation up to date with the latest releases.

Subscribe to repository notifications to receive alerts about security updates.
