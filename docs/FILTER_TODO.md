# Audit Log Action Types - Implementation Status

This document tracks all available audit log action types and their implementation status.

## Action Types Overview

| Action Type | Category | Status | Location |
|-------------|----------|--------|----------|
| `LOGIN_ATTEMPT` | Authentication | Implemented | `AuthenticationService.authenticate()` |
| `LOGIN_SUCCESS` | Authentication | Not Used | Use LOGIN_ATTEMPT with SUCCESS status |
| `LOGIN_FAILURE` | Authentication | Not Used | Use LOGIN_ATTEMPT with FAILURE status |
| `LOGOUT` | Authentication | Not Implemented | Needs implementation |
| `SESSION_EXPIRED` | Authentication | Not Implemented | Needs implementation |
| `TOKEN_REFRESH` | Authentication | Not Implemented | Needs implementation |
| `DOCUMENT_UPLOAD` | Document | Not Implemented | `DocumentController.uploadDocument()` |
| `DOCUMENT_DOWNLOAD` | Document | Not Implemented | `DocumentController.downloadDocument()` |
| `DOCUMENT_VIEW` | Document | Not Implemented | `DocumentController.getDocument()` |
| `DOCUMENT_DELETE` | Document | Not Implemented | `DocumentController.deleteDocument()` |
| `DOCUMENT_UPDATE` | Document | Not Implemented | Not yet created |
| `USER_CREATE` | Administrative | Implemented | `UserService.createUser()` |
| `USER_UPDATE` | Administrative | Not Implemented | Needs implementation |
| `USER_DELETE` | Administrative | Not Implemented | Needs implementation |
| `USER_ROLE_UPDATE` | Administrative | Implemented | `UserService.updateUserRoles()` |
| `USER_CLEARANCE_UPDATE` | Administrative | Implemented | `UserService.updateClearanceLevel()` |
| `USER_DEACTIVATE` | Administrative | Implemented | `UserService.deactivateUser()` |
| `USER_UNLOCK` | Administrative | Implemented | `UserService.unlockUser()` |
| `ROLE_ASSIGN` | Administrative | Not Implemented | Needs implementation |
| `ROLE_REVOKE` | Administrative | Not Implemented | Needs implementation |
| `SYSTEM_START` | System | Not Implemented | Application startup event |
| `SYSTEM_SHUTDOWN` | System | Not Implemented | Application shutdown event |
| `CONFIG_CHANGE` | System | Not Implemented | Needs implementation |
| `SECURITY_ALERT` | System | Not Implemented | Needs implementation |

## Implementation Details

### Currently Implemented (6 total)

1. **LOGIN_ATTEMPT** - `AuthenticationService.java:57`
   - Uses `@Auditable` annotation
   - Status becomes SUCCESS on successful login, FAILURE on exception

2. **USER_CREATE** - `UserService.java:79`
   - Uses `@Auditable` annotation

3. **USER_ROLE_UPDATE** - `UserService.java:134`
   - Uses `@Auditable` annotation with `resourceIdExpression`

4. **USER_CLEARANCE_UPDATE** - `UserService.java:148`
   - Uses `@Auditable` annotation with `resourceIdExpression`

5. **USER_DEACTIVATE** - `UserService.java:162`
   - Uses `@Auditable` annotation with `resourceIdExpression`

6. **USER_UNLOCK** - `UserService.java:176`
   - Uses `@Auditable` annotation with `resourceIdExpression`

### Needs Implementation (18 total)

#### High Priority - Document Operations
- [ ] `DOCUMENT_UPLOAD` - Add to DocumentController or DocumentService
- [ ] `DOCUMENT_DOWNLOAD` - Add to DocumentController
- [ ] `DOCUMENT_VIEW` - Add to DocumentController.getDocument()
- [ ] `DOCUMENT_DELETE` - Add to DocumentController.deleteDocument()

#### Medium Priority - Authentication Events
- [ ] `LOGOUT` - Add to AuthController.logout()
- [ ] `SESSION_EXPIRED` - Add to JwtAuthenticationFilter
- [ ] `TOKEN_REFRESH` - Add when refresh endpoint is implemented

#### Lower Priority - Administrative
- [ ] `USER_UPDATE` - Add to UserService when update method exists
- [ ] `USER_DELETE` - Add to UserService when delete method exists
- [ ] `ROLE_ASSIGN` - Covered by USER_ROLE_UPDATE
- [ ] `ROLE_REVOKE` - Covered by USER_ROLE_UPDATE

#### System Events (Optional)
- [ ] `SYSTEM_START` - Add ApplicationListener for ContextRefreshedEvent
- [ ] `SYSTEM_SHUTDOWN` - Add @PreDestroy or ApplicationListener
- [ ] `CONFIG_CHANGE` - Add when config management is implemented
- [ ] `SECURITY_ALERT` - Add for security-related events

## How to Add Audit Logging

### Method 1: Using @Auditable Annotation (Recommended for Services)

```java
@Auditable(
    action = ActionType.DOCUMENT_UPLOAD,
    resourceType = "DOCUMENT",
    resourceIdExpression = "#result.id"
)
public Document uploadDocument(MultipartFile file, User user) {
    // implementation
}
```

### Method 2: Manual Logging (For Controllers or Complex Logic)

```java
@Autowired
private AuditService auditService;

public void someMethod() {
    auditService.log(
        ActionType.DOCUMENT_DOWNLOAD,
        "DOCUMENT",
        documentId,
        AuditStatus.SUCCESS,
        "Downloaded by user",
        ipAddress,
        userAgent
    );
}
```

## Filter API Usage

The `/api/audit-logs` endpoint supports the following filters:

| Parameter | Type | Description |
|-----------|------|-------------|
| `actionType` | String | Filter by action type (e.g., `LOGIN_ATTEMPT`) |
| `userId` | String | Filter by user ID |
| `startDate` | String | Filter from date (YYYY-MM-DD) |
| `endDate` | String | Filter to date (YYYY-MM-DD) |
| `page` | Integer | Page number (default: 0) |
| `size` | Integer | Page size (default: 50) |

Example: `/api/audit-logs?actionType=DOCUMENT_UPLOAD&startDate=2024-01-01&endDate=2024-01-31`
