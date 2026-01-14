# Frontend TODO

## Completed

### Users Route (`/dashboard/users`) - 2026-01-13
- [x] Create user types and interfaces (`UserListItem`, `PaginatedResponse`, `UserFormData`)
- [x] Create `user-list.tsx` component with pagination
- [x] Create `user-form-modal.tsx` for create/edit (admin only)
- [x] Create `user-audit-modal.tsx` for viewing user audit logs (admin/auditor)
- [x] Create users page route at `/dashboard/users`
- [x] Add `put()` method to API utility
- [x] Update sidebar navigation to include Users link for all roles

#### Role Permissions Implemented
| Action | Admin | Officer | Auditor |
|--------|-------|---------|---------|
| View user list | Yes | Yes | Yes |
| Create users | Yes | No | No |
| Edit users / change roles | Yes | No | No |
| Enable/disable users | Yes | No | No |
| Delete users | Yes | No | No |
| View audit logs | Yes | No | Yes |

---

## In Progress

_None_

---

## Pending

### Future Enhancements
- [ ] Bulk user actions (enable/disable/delete multiple)
- [ ] User profile page with detailed info
- [ ] Password reset functionality
- [ ] MFA setup/reset for users
- [ ] Export users to CSV
- [ ] User activity dashboard/statistics
