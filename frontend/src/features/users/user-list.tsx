'use client';

import { useState } from 'react';
import { cn } from '@/util/utils';
import { Button, Input } from '@/components';
import type { UserRole } from '@/util/auth';

export interface UserListItem {
  id: string;
  email: string;
  fullName: string;
  roles: UserRole[];
  enabled: boolean;
  createdAt: string;
  lastLoginAt?: string;
}

export interface UserFilters {
  search?: string;
  role?: UserRole;
}

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

interface UserListProps {
  users: UserListItem[];
  isLoading?: boolean;
  currentPage: number;
  totalPages: number;
  totalElements: number;
  onPageChange: (page: number) => void;
  onFilter?: (filters: UserFilters) => void;
  onEdit?: (user: UserListItem) => void;
  onDelete?: (user: UserListItem) => void;
  onToggleEnabled?: (user: UserListItem) => void;
  onViewAuditLogs?: (user: UserListItem) => void;
  canManageUsers?: boolean;
  canViewAuditLogs?: boolean;
}

const roleColors: Record<UserRole, string> = {
  ADMINISTRATOR: 'bg-error-lighter text-error-dark border-error',
  OFFICER: 'bg-info-lighter text-info-dark border-info',
  AUDITOR: 'bg-warning-lighter text-warning-dark border-warning',
};

const roles: UserRole[] = ['ADMINISTRATOR', 'OFFICER', 'AUDITOR'];

function formatDate(dateString: string | undefined): string {
  if (!dateString) return 'Never';
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function UserList({
  users,
  isLoading,
  currentPage,
  totalPages,
  totalElements,
  onPageChange,
  onFilter,
  onEdit,
  onDelete,
  onToggleEnabled,
  onViewAuditLogs,
  canManageUsers,
  canViewAuditLogs,
}: UserListProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [roleFilter, setRoleFilter] = useState<UserRole | ''>('');

  const handleFilter = (e: React.FormEvent) => {
    e.preventDefault();
    onFilter?.({
      search: searchQuery || undefined,
      role: roleFilter || undefined,
    });
  };

  const handleClearFilters = () => {
    setSearchQuery('');
    setRoleFilter('');
    onFilter?.({});
  };

  const hasActiveFilters = searchQuery || roleFilter;

  const pageNumbers = [];
  const maxVisiblePages = 5;
  let startPage = Math.max(0, currentPage - Math.floor(maxVisiblePages / 2));
  const endPage = Math.min(totalPages, startPage + maxVisiblePages);

  if (endPage - startPage < maxVisiblePages) {
    startPage = Math.max(0, endPage - maxVisiblePages);
  }

  for (let i = startPage; i < endPage; i++) {
    pageNumbers.push(i);
  }

  return (
    <div className="space-y-4">
      {/* Search and filter form */}
      <form onSubmit={handleFilter} className="space-y-3" role="search">
        <div className="flex gap-2">
          <label htmlFor="user-search" className="sr-only">
            Search users
          </label>
          <Input
            id="user-search"
            type="search"
            placeholder="Search by name or email..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="flex-1"
          />
          <Button type="submit" variant="secondary">
            Search
          </Button>
        </div>

        <div className="flex flex-wrap gap-3 items-end">
          <div>
            <label htmlFor="role-filter" className="block text-sm font-medium text-neutral-700 mb-1">
              Role
            </label>
            <select
              id="role-filter"
              value={roleFilter}
              onChange={(e) => setRoleFilter(e.target.value as UserRole | '')}
              className="min-h-touch rounded border-2 border-neutral-400 bg-white px-3 py-2 text-neutral-900 focus:border-primary focus:outline-none focus:ring-focus focus:ring-primary focus:ring-offset-focus"
            >
              <option value="">All Roles</option>
              {roles.map((role) => (
                <option key={role} value={role}>{role}</option>
              ))}
            </select>
          </div>

          <Button type="submit" variant="primary">
            Apply Filters
          </Button>

          {hasActiveFilters && (
            <Button type="button" variant="ghost" onClick={handleClearFilters}>
              Clear Filters
            </Button>
          )}
        </div>
      </form>

      {/* Results count for screen readers */}
      <div aria-live="polite" className="sr-only">
        {isLoading
          ? 'Loading users...'
          : `${totalElements} users found, showing page ${currentPage + 1} of ${totalPages}`}
      </div>

      {/* User table */}
      <div className="overflow-x-auto rounded-lg border border-neutral-200">
        <table className="w-full text-left">
          <caption className="sr-only">List of users</caption>
          <thead className="border-b border-neutral-200 bg-neutral-50">
            <tr>
              <th scope="col" className="px-4 py-3 text-sm font-semibold text-neutral-900">
                User
              </th>
              <th scope="col" className="px-4 py-3 text-sm font-semibold text-neutral-900">
                Role
              </th>
              <th scope="col" className="px-4 py-3 text-sm font-semibold text-neutral-900">
                Status
              </th>
              <th scope="col" className="px-4 py-3 text-sm font-semibold text-neutral-900">
                Created
              </th>
              <th scope="col" className="px-4 py-3 text-sm font-semibold text-neutral-900">
                Last Login
              </th>
              <th scope="col" className="px-4 py-3 text-sm font-semibold text-neutral-900">
                <span className="sr-only">Actions</span>
              </th>
            </tr>
          </thead>
          <tbody className="divide-y divide-neutral-200">
            {isLoading ? (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center">
                  <div className="flex items-center justify-center gap-2">
                    <svg
                      className="h-5 w-5 animate-spin text-primary"
                      xmlns="http://www.w3.org/2000/svg"
                      fill="none"
                      viewBox="0 0 24 24"
                      aria-hidden="true"
                    >
                      <circle
                        className="opacity-25"
                        cx="12"
                        cy="12"
                        r="10"
                        stroke="currentColor"
                        strokeWidth="4"
                      />
                      <path
                        className="opacity-75"
                        fill="currentColor"
                        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
                      />
                    </svg>
                    <span className="text-neutral-500">Loading users...</span>
                  </div>
                </td>
              </tr>
            ) : users.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-neutral-500">
                  No users found
                </td>
              </tr>
            ) : (
              users.map((user) => (
                <tr key={user.id} className="hover:bg-neutral-50">
                  <td className="px-4 py-3">
                    <div>
                      <p className="font-medium text-neutral-900">{user.fullName}</p>
                      <p className="text-sm text-neutral-500">{user.email}</p>
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-1">
                      {user.roles.map((role) => (
                        <span
                          key={role}
                          className={cn(
                            'inline-block rounded border px-2 py-1 text-xs font-medium',
                            roleColors[role]
                          )}
                        >
                          {role}
                        </span>
                      ))}
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={cn(
                        'inline-block rounded border px-2 py-1 text-xs font-medium',
                        user.enabled
                          ? 'bg-success-lighter text-success-dark border-success'
                          : 'bg-neutral-200 text-neutral-600 border-neutral-400'
                      )}
                    >
                      {user.enabled ? 'Active' : 'Disabled'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm text-neutral-700">
                    {formatDate(user.createdAt)}
                  </td>
                  <td className="px-4 py-3 text-sm text-neutral-700">
                    {formatDate(user.lastLoginAt)}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex gap-1">
                      {canViewAuditLogs && (
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => onViewAuditLogs?.(user)}
                          aria-label={`View audit logs for ${user.fullName}`}
                        >
                          Logs
                        </Button>
                      )}
                      {canManageUsers && (
                        <>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => onEdit?.(user)}
                            aria-label={`Edit ${user.fullName}`}
                          >
                            Edit
                          </Button>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => onToggleEnabled?.(user)}
                            aria-label={user.enabled ? `Disable ${user.fullName}` : `Enable ${user.fullName}`}
                          >
                            {user.enabled ? 'Disable' : 'Enable'}
                          </Button>
                          <Button
                            variant="danger"
                            size="sm"
                            onClick={() => onDelete?.(user)}
                            aria-label={`Delete ${user.fullName}`}
                          >
                            Delete
                          </Button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <nav aria-label="User list pagination" className="flex items-center justify-between">
          <p className="text-sm text-neutral-600">
            Showing {currentPage * 10 + 1} to {Math.min((currentPage + 1) * 10, totalElements)} of{' '}
            {totalElements} users
          </p>
          <div className="flex gap-1">
            <Button
              variant="secondary"
              size="sm"
              onClick={() => onPageChange(0)}
              disabled={currentPage === 0}
              aria-label="Go to first page"
            >
              First
            </Button>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => onPageChange(currentPage - 1)}
              disabled={currentPage === 0}
              aria-label="Go to previous page"
            >
              Previous
            </Button>
            {pageNumbers.map((page) => (
              <Button
                key={page}
                variant={page === currentPage ? 'primary' : 'secondary'}
                size="sm"
                onClick={() => onPageChange(page)}
                aria-label={`Go to page ${page + 1}`}
                aria-current={page === currentPage ? 'page' : undefined}
              >
                {page + 1}
              </Button>
            ))}
            <Button
              variant="secondary"
              size="sm"
              onClick={() => onPageChange(currentPage + 1)}
              disabled={currentPage >= totalPages - 1}
              aria-label="Go to next page"
            >
              Next
            </Button>
            <Button
              variant="secondary"
              size="sm"
              onClick={() => onPageChange(totalPages - 1)}
              disabled={currentPage >= totalPages - 1}
              aria-label="Go to last page"
            >
              Last
            </Button>
          </div>
        </nav>
      )}
    </div>
  );
}
