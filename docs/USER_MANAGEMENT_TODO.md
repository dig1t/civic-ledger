# User Management Feature TODO

## Completed - 2026-01-13

### Backend API Endpoints
- [x] `GET /api/users?page={page}&size={size}&search={query}` - Paginated user list with search
- [x] `GET /api/users/{id}` - Get single user by ID
- [x] `POST /api/users` - Create new user (ADMINISTRATOR only)
- [x] `PUT /api/users/{id}` - Update user (ADMINISTRATOR only)
- [x] `PUT /api/users/{id}/status` - Enable/disable user (ADMINISTRATOR only)
- [x] `DELETE /api/users/{id}` - Delete user (ADMINISTRATOR only)

### Backend Components Created
- [x] `UserController.java` - REST controller with all CRUD endpoints
- [x] `UserDTO.java` - User data transfer object
- [x] `CreateUserRequest.java` - Request DTO for user creation
- [x] `UpdateUserRequest.java` - Request DTO for user updates
- [x] `UpdateUserStatusRequest.java` - Request DTO for enable/disable
- [x] `PagedResponse.java` - Generic paginated response DTO
- [x] Updated `UserService.java` with new methods:
  - `findAllPaginated(Pageable)`
  - `searchUsers(String, Pageable)`
  - `updateUser(UUID, String, String, String, String, Set<Role>)`
  - `activateUser(UUID)`
  - `deleteUser(UUID)`
  - `isEmailAvailableForUser(String, UUID)`
- [x] Updated `UserRepository.java` with:
  - `Page<User> findAll(Pageable)`
  - `Page<User> searchUsers(String, Pageable)`
- [x] Updated `SecurityConfig.java` with user endpoint authorization

### Frontend Components Created
- [x] `/dashboard/users` page route
- [x] `UserList` component with pagination
- [x] `UserFormModal` for create/edit
- [x] `UserAuditModal` for viewing user audit logs
- [x] Updated sidebar navigation for all roles

### Unit Tests
- [x] `UserControllerTest.java` - 36 tests covering:
  - List users with pagination and search
  - Get single user
  - Create user (validation, role-based access)
  - Update user (validation, email uniqueness)
  - Update user status (enable/disable)
  - Delete user
  - Role-based access control (ADMINISTRATOR, OFFICER, AUDITOR)

### Role-Based Access Control
| Endpoint | Admin | Officer | Auditor |
|----------|-------|---------|---------|
| GET /api/users | Yes | Yes | Yes |
| GET /api/users/{id} | Yes | Yes | Yes |
| POST /api/users | Yes | No | No |
| PUT /api/users/{id} | Yes | No | No |
| PUT /api/users/{id}/status | Yes | No | No |
| DELETE /api/users/{id} | Yes | No | No |

---

## Pending

### Future Enhancements
- [ ] Bulk user actions (enable/disable/delete multiple)
- [ ] User profile page with detailed info
- [ ] Password reset functionality
- [ ] MFA setup/reset for users
- [ ] Export users to CSV
- [ ] User activity dashboard/statistics
- [ ] Audit log for the `/api/audit-logs?userId={id}` endpoint with pagination (already exists)
