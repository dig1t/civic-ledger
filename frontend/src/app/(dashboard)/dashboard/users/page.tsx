'use client';

import { useCallback, useEffect, useState } from 'react';
import { useAuth, useRequireAuth } from '@/util/auth';
import { api, ApiError } from '@/util/api';
import { Button, useToast } from '@/components';
import {
  UserList,
  UserFormModal,
  UserAuditModal,
  type UserListItem,
  type PaginatedResponse,
  type UserFormData,
} from '@/features/users';

export default function UsersPage() {
  // All authenticated users can view this page
  const { isAuthorized, isLoading: authLoading } = useRequireAuth();
  const { hasRole } = useAuth();
  const { showToast } = useToast();

  const [users, setUsers] = useState<UserListItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');

  // Modal states
  const [isFormModalOpen, setIsFormModalOpen] = useState(false);
  const [isAuditModalOpen, setIsAuditModalOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<UserListItem | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Delete confirmation
  const [userToDelete, setUserToDelete] = useState<UserListItem | null>(null);

  // Permission checks
  const canManageUsers = hasRole('ADMINISTRATOR');
  const canViewAuditLogs = hasRole('ADMINISTRATOR', 'AUDITOR');

  const fetchUsers = useCallback(async (page: number, search?: string) => {
    setIsLoading(true);
    try {
      let endpoint = `/users?page=${page}&size=10`;
      if (search) {
        endpoint += `&search=${encodeURIComponent(search)}`;
      }
      const response = await api.get<PaginatedResponse<UserListItem>>(endpoint);
      setUsers(response.content);
      setCurrentPage(response.page);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Failed to load users';
      showToast(message, 'error');
    } finally {
      setIsLoading(false);
    }
  }, [showToast]);

  useEffect(() => {
    if (isAuthorized) {
      fetchUsers(0, searchQuery);
    }
  }, [isAuthorized, fetchUsers, searchQuery]);

  const handlePageChange = (page: number) => {
    fetchUsers(page, searchQuery);
  };

  const handleSearch = (query: string) => {
    setSearchQuery(query);
    fetchUsers(0, query);
  };

  const handleCreateUser = () => {
    setSelectedUser(null);
    setIsFormModalOpen(true);
  };

  const handleEditUser = (user: UserListItem) => {
    setSelectedUser(user);
    setIsFormModalOpen(true);
  };

  const handleViewAuditLogs = (user: UserListItem) => {
    setSelectedUser(user);
    setIsAuditModalOpen(true);
  };

  const handleFormSubmit = async (data: UserFormData) => {
    setIsSubmitting(true);
    try {
      if (selectedUser) {
        // Update existing user
        await api.put(`/users/${selectedUser.id}`, data);
        showToast('User updated successfully', 'success');
      } else {
        // Create new user
        await api.post('/users', data);
        showToast('User created successfully', 'success');
      }
      setIsFormModalOpen(false);
      fetchUsers(currentPage, searchQuery);
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Operation failed';
      showToast(message, 'error');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleToggleEnabled = async (user: UserListItem) => {
    try {
      await api.put(`/users/${user.id}/status`, { enabled: !user.enabled });
      showToast(
        user.enabled ? 'User disabled successfully' : 'User enabled successfully',
        'success'
      );
      fetchUsers(currentPage, searchQuery);
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Operation failed';
      showToast(message, 'error');
    }
  };

  const handleDeleteUser = async (user: UserListItem) => {
    setUserToDelete(user);
  };

  const confirmDelete = async () => {
    if (!userToDelete) return;

    try {
      await api.delete(`/users/${userToDelete.id}`);
      showToast('User deleted successfully', 'success');
      setUserToDelete(null);
      fetchUsers(currentPage, searchQuery);
    } catch (err) {
      const message = err instanceof ApiError ? err.message : 'Failed to delete user';
      showToast(message, 'error');
    }
  };

  if (authLoading || !isAuthorized) {
    return null;
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900">Users</h1>
          <p className="mt-1 text-neutral-600">
            {canManageUsers
              ? 'Manage system users, roles, and permissions'
              : 'View system users'}
          </p>
        </div>
        {canManageUsers && (
          <Button onClick={handleCreateUser}>
            Create User
          </Button>
        )}
      </div>

      <UserList
        users={users}
        isLoading={isLoading}
        currentPage={currentPage}
        totalPages={totalPages}
        totalElements={totalElements}
        onPageChange={handlePageChange}
        onSearch={handleSearch}
        onEdit={handleEditUser}
        onDelete={handleDeleteUser}
        onToggleEnabled={handleToggleEnabled}
        onViewAuditLogs={handleViewAuditLogs}
        canManageUsers={canManageUsers}
        canViewAuditLogs={canViewAuditLogs}
      />

      {/* Create/Edit User Modal */}
      <UserFormModal
        isOpen={isFormModalOpen}
        onClose={() => setIsFormModalOpen(false)}
        onSubmit={handleFormSubmit}
        user={selectedUser}
        isLoading={isSubmitting}
      />

      {/* User Audit Logs Modal */}
      <UserAuditModal
        isOpen={isAuditModalOpen}
        onClose={() => setIsAuditModalOpen(false)}
        user={selectedUser}
      />

      {/* Delete Confirmation Dialog */}
      {userToDelete && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
          role="dialog"
          aria-modal="true"
          aria-labelledby="delete-dialog-title"
        >
          <div className="w-full max-w-sm rounded-lg bg-white p-6 shadow-xl">
            <h2 id="delete-dialog-title" className="text-lg font-semibold text-neutral-900">
              Delete User
            </h2>
            <p className="mt-2 text-neutral-600">
              Are you sure you want to delete <strong>{userToDelete.fullName}</strong>? This action
              cannot be undone.
            </p>
            <div className="mt-6 flex justify-end gap-2">
              <Button variant="secondary" onClick={() => setUserToDelete(null)}>
                Cancel
              </Button>
              <Button variant="danger" onClick={confirmDelete}>
                Delete
              </Button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
