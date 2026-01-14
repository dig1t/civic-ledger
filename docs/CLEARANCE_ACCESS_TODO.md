# Clearance-Based Document Access Control Implementation

## Overview

This document outlines the implementation tasks for enforcing clearance-based document access control in CivicLedger. Currently, the system has classification levels defined for documents and clearance levels for users, but enforcement is not implemented - any OFFICER can access any document regardless of their clearance level.

**Security Gap:** The `DocumentController` does not validate user clearance against document classification before granting access.

**NIST 800-53 Controls:** AC-3 (Access Enforcement), AC-6 (Least Privilege), AU-2 (Audit Events)

---

## Classification Level Hierarchy

```
TOP_SECRET > SECRET > CONFIDENTIAL > CUI > UNCLASSIFIED
```

A user with `SECRET` clearance can access documents classified as `SECRET`, `CONFIDENTIAL`, `CUI`, and `UNCLASSIFIED`, but NOT `TOP_SECRET`.

---

## Phase 1: Core Security Components

### 1.1 Create ClearanceValidator Service

**File:** `src/main/java/com/civicledger/security/ClearanceValidator.java`

- [x] Create `ClearanceValidator` class in the security package
- [x] Implement `hasAccess(User user, Document document)` method returning boolean
- [x] Implement `hasAccess(ClassificationLevel userClearance, ClassificationLevel docClassification)` method
- [x] Implement `getAccessibleLevels(ClassificationLevel userClearance)` method returning list of accessible levels
- [x] Add `@Service` annotation for Spring dependency injection
- [x] Handle null clearance level (treat as UNCLASSIFIED)
- [x] Add comprehensive Javadoc documentation

**Implementation Notes:**
```java
// Clearance validation logic uses enum ordinal comparison
// ClassificationLevel enum order: UNCLASSIFIED(0), CUI(1), CONFIDENTIAL(2), SECRET(3), TOP_SECRET(4)
// User can access document if: userClearance.ordinal() >= docClassification.ordinal()
```

### 1.2 Create InsufficientClearanceException

**File:** `src/main/java/com/civicledger/exception/InsufficientClearanceException.java`

- [x] Create custom exception extending `RuntimeException`
- [x] Add constructor with message parameter
- [x] Add constructor with user clearance and required clearance parameters
- [x] Add `getUserClearance()` getter
- [x] Add `getRequiredClearance()` getter
- [x] Add `getDocumentId()` getter (optional field)
- [x] Implement meaningful `getMessage()` override

**Example:**
```java
public class InsufficientClearanceException extends RuntimeException {
    private final ClassificationLevel userClearance;
    private final ClassificationLevel requiredClearance;
    private final UUID documentId;
    // ...
}
```

### 1.3 Add DOCUMENT_ACCESS_DENIED Audit Action Type

**File:** `src/main/java/com/civicledger/entity/AuditLog.java`

- [x] Add `DOCUMENT_ACCESS_DENIED` to `ActionType` enum (after `DOCUMENT_UPDATE`)
- [x] Ensure proper documentation comment for the new action type

**Location in enum:**
```java
// Document operations
DOCUMENT_UPLOAD,
DOCUMENT_DOWNLOAD,
DOCUMENT_VIEW,
DOCUMENT_DELETE,
DOCUMENT_UPDATE,
DOCUMENT_ACCESS_DENIED,  // <-- Add here
```

---

## Phase 2: Exception Handling

### 2.1 Create or Update GlobalExceptionHandler

**File:** `src/main/java/com/civicledger/exception/GlobalExceptionHandler.java`

- [x] Create `GlobalExceptionHandler` class if it does not exist
- [x] Add `@RestControllerAdvice` annotation
- [x] Implement `handleInsufficientClearance(InsufficientClearanceException ex)` method
- [x] Return HTTP 403 Forbidden status
- [x] Return structured error response with classification details (without exposing sensitive info)
- [x] Log the access denial attempt
- [x] Ensure response does not leak document existence information

**Response Format:**
```json
{
  "error": "ACCESS_DENIED",
  "message": "Insufficient clearance level to access this resource",
  "timestamp": "2026-01-13T10:30:00Z"
}
```

---

## Phase 3: Controller Updates

### 3.1 Update DocumentController - View Endpoint

**File:** `src/main/java/com/civicledger/controller/DocumentController.java`

**Method:** `getDocument(@PathVariable UUID id, ...)`

- [x] Inject `ClearanceValidator` into controller constructor
- [x] Retrieve authenticated user from `@AuthenticationPrincipal`
- [x] After finding document, call `clearanceValidator.hasAccess(user, document)`
- [x] If access denied, log audit event with `DOCUMENT_ACCESS_DENIED`
- [x] Throw `InsufficientClearanceException` if access denied
- [x] Update audit log details to include clearance validation result

### 3.2 Update DocumentController - Download Endpoint

**File:** `src/main/java/com/civicledger/controller/DocumentController.java`

**Method:** `downloadDocument(@PathVariable UUID id, ...)`

- [x] Add `@AuthenticationPrincipal User user` parameter
- [x] Validate clearance before retrieving encrypted data
- [x] Log `DOCUMENT_ACCESS_DENIED` audit event on failure
- [x] Throw `InsufficientClearanceException` if access denied
- [x] Ensure denial happens before any decryption operations (fail fast)

### 3.3 Update DocumentController - List Endpoint

**File:** `src/main/java/com/civicledger/controller/DocumentController.java`

**Method:** `listDocuments(...)`

- [x] Add `@AuthenticationPrincipal User user` parameter
- [x] Filter documents by user's clearance level using repository query
- [x] Replace `findByDeletedFalse()` with `findByClassificationLevelAtOrBelow()`
- [x] Update search query to include clearance filtering
- [x] Ensure pagination works correctly with filtered results

---

## Phase 4: Service Layer Updates

### 4.1 Create or Update DocumentService

**File:** `src/main/java/com/civicledger/service/DocumentService.java`

- [ ] Create `DocumentService` class if it does not exist
- [ ] Inject `DocumentRepository` and `ClearanceValidator`
- [ ] Implement `getDocument(UUID id, User user)` with clearance check
- [ ] Implement `listDocuments(User user, Pageable pageable)` with clearance filtering
- [ ] Implement `searchDocuments(String query, User user, Pageable pageable)` with clearance filtering
- [ ] Implement `downloadDocument(UUID id, User user)` with clearance check
- [ ] Move business logic from controller to service layer

### 4.2 Update DocumentRepository Queries

**File:** `src/main/java/com/civicledger/repository/DocumentRepository.java`

- [x] Add search query with clearance filter:
  ```java
  @Query("SELECT d FROM Document d WHERE d.deleted = false AND d.classificationLevel <= :maxLevel AND LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :query, '%'))")
  Page<Document> searchByFilenameWithClearance(@Param("query") String query, @Param("maxLevel") ClassificationLevel maxLevel, Pageable pageable);
  ```
- [x] Verify `findByClassificationLevelAtOrBelow()` query works correctly with enum ordinal comparison

---

## Phase 5: User Entity Updates

### 5.1 Make User Clearance Required with Default

**File:** `src/main/java/com/civicledger/entity/User.java`

- [x] Add `@Column(nullable = false)` to `clearanceLevel` field
- [x] Add `@Builder.Default` annotation with `UNCLASSIFIED` default
- [x] Update existing code to handle the non-null requirement

**Updated field:**
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
@Builder.Default
private ClassificationLevel clearanceLevel = ClassificationLevel.UNCLASSIFIED;
```

### 5.2 Create Database Migration

**File:** `src/main/resources/db/migration/V{next}_add_default_clearance.sql`

- [ ] Add migration to set default clearance for existing users
- [ ] Update column to NOT NULL after setting defaults

**Migration SQL:**
```sql
-- Set default clearance for existing users without clearance
UPDATE users SET clearance_level = 'UNCLASSIFIED' WHERE clearance_level IS NULL;

-- Make column NOT NULL
ALTER TABLE users ALTER COLUMN clearance_level SET NOT NULL;
ALTER TABLE users ALTER COLUMN clearance_level SET DEFAULT 'UNCLASSIFIED';
```

---

## Phase 6: Testing

### 6.1 Unit Tests - ClearanceValidator

**File:** `src/test/java/com/civicledger/security/ClearanceValidatorTest.java`

- [ ] Test `hasAccess()` returns true when clearance equals classification
- [ ] Test `hasAccess()` returns true when clearance exceeds classification
- [ ] Test `hasAccess()` returns false when clearance is below classification
- [ ] Test null user clearance defaults to UNCLASSIFIED
- [ ] Test all classification level combinations (5x5 matrix)
- [ ] Test `getAccessibleLevels()` returns correct list for each clearance

### 6.2 Unit Tests - InsufficientClearanceException

**File:** `src/test/java/com/civicledger/exception/InsufficientClearanceExceptionTest.java`

- [ ] Test exception message formatting
- [ ] Test getters return correct values
- [ ] Test exception with null document ID

### 6.3 Integration Tests - DocumentController

**File:** `src/test/java/com/civicledger/controller/DocumentControllerTest.java`

- [ ] Test view document with sufficient clearance returns 200
- [ ] Test view document with insufficient clearance returns 403
- [ ] Test download document with sufficient clearance returns 200
- [ ] Test download document with insufficient clearance returns 403
- [ ] Test list documents only returns documents at or below user clearance
- [ ] Test search documents respects clearance filtering
- [ ] Test UNCLASSIFIED user can only see UNCLASSIFIED documents
- [ ] Test TOP_SECRET user can see all documents
- [ ] Test audit log records DOCUMENT_ACCESS_DENIED events

### 6.4 Integration Tests - Audit Logging

**File:** `src/test/java/com/civicledger/service/AuditServiceTest.java`

- [ ] Test DOCUMENT_ACCESS_DENIED audit log is created on denial
- [ ] Test audit log contains user clearance level
- [ ] Test audit log contains required clearance level
- [ ] Test audit log contains document ID (without exposing content)

### 6.5 Security Tests

**File:** `src/test/java/com/civicledger/security/ClearanceAccessSecurityTest.java`

- [ ] Test that clearance check happens before document retrieval from storage
- [ ] Test that error messages do not reveal document existence
- [ ] Test that timing attacks cannot reveal document existence
- [ ] Test concurrent access attempts are properly handled

---

## Phase 7: Documentation

### 7.1 Update API Documentation

**File:** `docs/api/documents.md`

- [ ] Document clearance requirements for each endpoint
- [ ] Document 403 error response format
- [ ] Add examples of clearance-based filtering

### 7.2 Update Security Documentation

**File:** `docs/security/access-control.md`

- [ ] Document classification level hierarchy
- [ ] Document clearance validation logic
- [ ] Add NIST 800-53 control mapping

---

## Implementation Order

1. **Phase 1.2** - Create `InsufficientClearanceException` (no dependencies)
2. **Phase 1.3** - Add `DOCUMENT_ACCESS_DENIED` to AuditLog enum (no dependencies)
3. **Phase 1.1** - Create `ClearanceValidator` service (depends on exception)
4. **Phase 2.1** - Update `GlobalExceptionHandler` (depends on exception)
5. **Phase 5.1** - Update User entity (no dependencies)
6. **Phase 5.2** - Create database migration (depends on entity change)
7. **Phase 4.2** - Update repository queries (no dependencies)
8. **Phase 4.1** - Create/update `DocumentService` (depends on validator, repository)
9. **Phase 3.1-3.3** - Update `DocumentController` (depends on service, validator)
10. **Phase 6** - Write tests (depends on all implementation)
11. **Phase 7** - Update documentation (depends on all implementation)

---

## Acceptance Criteria

- [ ] Users can only view documents at or below their clearance level
- [ ] Users can only download documents at or below their clearance level
- [ ] Document list only shows documents at or below user's clearance level
- [ ] Search results are filtered by user's clearance level
- [ ] All access denials are logged with DOCUMENT_ACCESS_DENIED action type
- [ ] Access denial returns HTTP 403 without revealing document existence
- [ ] Users without explicit clearance default to UNCLASSIFIED
- [ ] All existing tests pass
- [ ] New tests achieve >90% coverage of new code
- [ ] Documentation is updated

---

## Risk Considerations

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Existing users have NULL clearance | High | Medium | Database migration sets default to UNCLASSIFIED |
| Performance impact from clearance checks | Low | Low | Clearance check is in-memory enum comparison |
| Breaking existing API consumers | Medium | High | 403 response is standard; document behavior changes |
| Audit log volume increase | Low | Low | Only log denials, not successful access |

---

## Related Files

**Modified:**
- `src/main/java/com/civicledger/controller/DocumentController.java`
- `src/main/java/com/civicledger/entity/AuditLog.java`
- `src/main/java/com/civicledger/entity/User.java`
- `src/main/java/com/civicledger/repository/DocumentRepository.java`

**Created:**
- `src/main/java/com/civicledger/security/ClearanceValidator.java`
- `src/main/java/com/civicledger/exception/InsufficientClearanceException.java`
- `src/main/java/com/civicledger/exception/GlobalExceptionHandler.java`
- `src/main/java/com/civicledger/service/DocumentService.java`
- `src/main/resources/db/migration/V{next}_add_default_clearance.sql`
- `src/test/java/com/civicledger/security/ClearanceValidatorTest.java`
- `src/test/java/com/civicledger/exception/InsufficientClearanceExceptionTest.java`
- `src/test/java/com/civicledger/controller/DocumentControllerTest.java`
- `src/test/java/com/civicledger/security/ClearanceAccessSecurityTest.java`

---

## Estimated Effort

| Phase | Effort |
|-------|--------|
| Phase 1: Core Security Components | 4 hours |
| Phase 2: Exception Handling | 1 hour |
| Phase 3: Controller Updates | 3 hours |
| Phase 4: Service Layer Updates | 2 hours |
| Phase 5: User Entity Updates | 1 hour |
| Phase 6: Testing | 4 hours |
| Phase 7: Documentation | 2 hours |
| **Total** | **17 hours** |
