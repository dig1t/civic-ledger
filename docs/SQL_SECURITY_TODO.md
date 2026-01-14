# SQL Query Security Audit

## Overview

Checklist for verifying all SQL queries are safely constructed and protected against injection attacks. Aligns with NIST 800-53 SI-10 (Information Input Validation) and OWASP SQL Injection Prevention.

---

## Audit Checklist

### 1. Repository Layer Review

**Spring Data JPA Repositories:**
- [ ] All `@Query` annotations use parameterized queries (`:paramName` or `?1` syntax)
- [ ] No string concatenation in JPQL/HQL queries
- [ ] No `@Query` with `nativeQuery = true` using string concatenation
- [ ] Verify `@Param` annotations match query parameters
- [ ] Check `Specification` and `Criteria` API usage for dynamic queries

**Files to audit:**
- [ ] `DocumentRepository.java`
- [ ] `UserRepository.java`
- [ ] `AuditLogRepository.java`
- [ ] Any other `*Repository.java` files

### 2. Service Layer Review

- [ ] No raw SQL or JDBC usage bypassing JPA
- [ ] No `EntityManager.createNativeQuery()` with string concatenation
- [ ] No `JdbcTemplate` with string concatenation
- [ ] Verify any dynamic query building uses `CriteriaBuilder`

### 3. Controller Layer Review

- [ ] User input is validated before reaching repository
- [ ] `@Valid` annotation on request bodies
- [ ] Path variables and query params are type-safe (UUID, Integer, Enum)
- [ ] No user input directly used in sort/order clauses
- [ ] Pagination parameters are validated (max page size limits)

### 4. Input Validation

- [ ] String inputs have length limits
- [ ] Search queries are sanitized or use parameterized LIKE clauses
- [ ] Enum parameters use `@ValidEnum` or type conversion
- [ ] Date/time inputs use proper parsing (no string manipulation)
- [ ] File names are sanitized before any query usage

### 5. Native Query Audit

If native queries exist:
- [ ] Document why native query is necessary
- [ ] Verify all parameters use `?` placeholders or named parameters
- [ ] No dynamic table/column names from user input
- [ ] Review for database-specific injection vectors

### 6. Logging Review

- [ ] SQL logging does not expose sensitive data in production
- [ ] Query parameters are not logged in plain text
- [ ] Error messages don't reveal query structure to users

---

## Common Vulnerable Patterns to Find

```java
// BAD: String concatenation in query
@Query("SELECT u FROM User u WHERE u.name = '" + name + "'")

// BAD: String concatenation in native query
@Query(value = "SELECT * FROM users WHERE name = '" + name + "'", nativeQuery = true)

// BAD: Dynamic sort without validation
Sort.by(userProvidedSortField)

// BAD: LIKE with unescaped input
@Query("SELECT d FROM Document d WHERE d.name LIKE '%" + search + "%'")
```

```java
// GOOD: Parameterized query
@Query("SELECT u FROM User u WHERE u.name = :name")

// GOOD: Safe LIKE clause
@Query("SELECT d FROM Document d WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :search, '%'))")

// GOOD: Validated sort fields
if (ALLOWED_SORT_FIELDS.contains(sortField)) { ... }
```

---

## Tests to Write

### Unit Tests

- [ ] `SqlInjectionPreventionTest.java` - Verify parameterized queries
  - [ ] Test search with SQL injection payloads (`'; DROP TABLE users; --`)
  - [ ] Test search with UNION-based injection attempts
  - [ ] Test special characters in search (`%`, `_`, `'`, `"`, `\`)
  - [ ] Test null byte injection (`%00`)

### Integration Tests

- [ ] `RepositorySecurityTest.java`
  - [ ] Test each repository method with malicious input
  - [ ] Verify queries execute safely without errors
  - [ ] Verify no unintended data exposure

### Test Payloads

```java
// Common SQL injection test payloads
String[] payloads = {
    "' OR '1'='1",
    "'; DROP TABLE users; --",
    "1; SELECT * FROM users",
    "' UNION SELECT * FROM users --",
    "admin'--",
    "1' AND '1'='1",
    "%' AND 1=1 --",
    "' OR 1=1 #",
    "'; EXEC xp_cmdshell('dir'); --",
    "1' WAITFOR DELAY '0:0:5' --"
};
```

---

## Verification Steps

1. [ ] Run static analysis (SpotBugs, SonarQube SQL rules)
2. [ ] Grep codebase for string concatenation in queries
3. [ ] Review all `@Query` annotations manually
4. [ ] Run SQL injection tests against each endpoint
5. [ ] Enable Hibernate SQL logging and review generated queries
6. [ ] Test with OWASP ZAP or sqlmap (in test environment only)

---

## Grep Commands for Audit

```bash
# Find all @Query annotations
grep -rn "@Query" src/main/java/

# Find potential string concatenation in queries
grep -rn "\".*+.*\"" src/main/java/ | grep -i "query\|select\|from\|where"

# Find native queries
grep -rn "nativeQuery.*=.*true" src/main/java/

# Find EntityManager usage
grep -rn "createNativeQuery\|createQuery" src/main/java/

# Find JdbcTemplate usage
grep -rn "JdbcTemplate\|NamedParameterJdbcTemplate" src/main/java/
```

---

## Sign-off

- [ ] All repositories audited
- [ ] All tests written and passing
- [ ] Static analysis clean
- [ ] Manual penetration testing completed
- [ ] Documentation updated

**Audited by:** _______________
**Date:** _______________
