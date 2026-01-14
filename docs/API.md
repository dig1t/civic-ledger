# CivicLedger API Documentation

## Overview

CivicLedger provides a secure REST API for document management with full audit logging. All endpoints require JWT authentication unless otherwise noted.

**Base URL:** `http://localhost:8080`

**OpenAPI/Swagger:** Available at `/swagger-ui.html`

## Authentication

### Login
```
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.gov",
  "password": "password",
  "mfaCode": "123456"  // Optional for mock mode
}
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": {
    "id": "uuid",
    "email": "user@example.gov",
    "fullName": "John Doe",
    "roles": ["OFFICER"]
  }
}
```

### Using the Token
Include the token in the Authorization header:
```
Authorization: Bearer <accessToken>
```

## Documents API

### List Documents
```
GET /api/documents?page=0&size=20&search=keyword
```
Requires: `OFFICER` or `ADMINISTRATOR` role

### Get Document
```
GET /api/documents/{id}
```
Requires: `OFFICER` or `ADMINISTRATOR` role

### Upload Document
```
POST /api/documents/upload
Content-Type: multipart/form-data

file: (binary)
description: "Document description"
classificationLevel: "UNCLASSIFIED" | "CONFIDENTIAL" | "SECRET" | "TOP_SECRET"
```
Requires: `OFFICER` or `ADMINISTRATOR` role

**Security:** File is hashed with SHA-256, encrypted with AES-256-GCM before storage.

### Download Document
```
GET /api/documents/{id}/download
```
Requires: `OFFICER` or `ADMINISTRATOR` role

**Security:** File is decrypted and hash verified before delivery.

### Delete Document
```
DELETE /api/documents/{id}
```
Requires: `OFFICER` or `ADMINISTRATOR` role

## Audit Logs API

### List Audit Logs
```
GET /api/audit-logs?actionType=LOGIN_SUCCESS&userId=user1&startDate=2024-01-01&endDate=2024-12-31&page=0&size=50
```
Requires: `AUDITOR` or `ADMINISTRATOR` role

**Filter Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| actionType | String | Filter by action (LOGIN_SUCCESS, DOCUMENT_UPLOAD, etc.) |
| userId | String | Filter by user ID |
| startDate | String | Start date (YYYY-MM-DD) |
| endDate | String | End date (YYYY-MM-DD) |
| page | Integer | Page number (default: 0) |
| size | Integer | Page size (default: 50) |

### Get Audit Trail for Resource
```
GET /api/audit-logs/resource/{resourceType}/{resourceId}
```
Requires: `AUDITOR` or `ADMINISTRATOR` role

## Health Endpoints

### Basic Health
```
GET /api/health
```
Public endpoint.

### Kubernetes Liveness Probe
```
GET /health/live
```
Public endpoint. Returns 200 if application is running.

### Kubernetes Readiness Probe
```
GET /health/ready
```
Public endpoint. Returns 200 if application is ready (database connected).

## Action Types

| Action Type | Description |
|-------------|-------------|
| LOGIN_SUCCESS | Successful login |
| LOGIN_FAILURE | Failed login attempt |
| LOGOUT | User logout |
| DOCUMENT_UPLOAD | Document uploaded |
| DOCUMENT_DOWNLOAD | Document downloaded |
| DOCUMENT_VIEW | Document metadata viewed |
| DOCUMENT_DELETE | Document deleted |
| USER_CREATE | New user created |
| USER_ROLE_UPDATE | User roles updated |
| USER_DEACTIVATE | User deactivated |

## Error Responses

| Status | Description |
|--------|-------------|
| 400 | Bad Request - Invalid parameters |
| 401 | Unauthorized - Missing or invalid token |
| 403 | Forbidden - Insufficient permissions |
| 404 | Not Found - Resource doesn't exist |
| 500 | Internal Server Error |
