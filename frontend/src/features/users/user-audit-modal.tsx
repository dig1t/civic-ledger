'use client';

import { useEffect, useRef, useState, useCallback } from 'react';
import { cn } from '@/util/utils';
import { Button } from '@/components';
import { api } from '@/util/api';
import type { UserListItem, PaginatedResponse } from './user-list';

export interface UserAuditLogEntry {
  id: string;
  actionType: string;
  userId: string;
  userName?: string;
  resourceId?: string;
  resourceName?: string;
  resourceType?: string;
  ipAddress: string;
  timestamp: string;
  details?: string;
  status?: string;
}

interface UserAuditModalProps {
  isOpen: boolean;
  onClose: () => void;
  user: UserListItem | null;
}

const actionTypeLabels: Record<string, string> = {
  DOCUMENT_UPLOAD: 'Document Upload',
  DOCUMENT_DOWNLOAD: 'Document Download',
  DOCUMENT_VIEW: 'Document View',
  DOCUMENT_DELETE: 'Document Delete',
  DOCUMENT_UPDATE: 'Document Update',
  LOGIN_ATTEMPT: 'Login Attempt',
  LOGIN_SUCCESS: 'Login Success',
  LOGIN_FAILURE: 'Login Failure',
  LOGOUT: 'Logout',
  SESSION_EXPIRED: 'Session Expired',
  USER_CREATE: 'User Created',
  USER_UPDATE: 'User Updated',
  USER_DELETE: 'User Deleted',
  USER_DEACTIVATE: 'User Deactivated',
  ROLE_ASSIGN: 'Role Assigned',
  ROLE_REVOKE: 'Role Revoked',
  SYSTEM_START: 'System Start',
  SECURITY_ALERT: 'Security Alert',
};

const actionTypeColors: Record<string, string> = {
  DOCUMENT_UPLOAD: 'bg-success-lighter text-success-dark border-success',
  DOCUMENT_DOWNLOAD: 'bg-info-lighter text-info-dark border-info',
  DOCUMENT_VIEW: 'bg-info-lighter text-info-dark border-info',
  DOCUMENT_DELETE: 'bg-error-lighter text-error-dark border-error',
  DOCUMENT_UPDATE: 'bg-warning-lighter text-warning-dark border-warning',
  LOGIN_ATTEMPT: 'bg-primary-lighter text-primary-dark border-primary',
  LOGIN_SUCCESS: 'bg-success-lighter text-success-dark border-success',
  LOGIN_FAILURE: 'bg-error-lighter text-error-dark border-error',
  LOGOUT: 'bg-neutral-200 text-neutral-700 border-neutral-400',
  SESSION_EXPIRED: 'bg-warning-lighter text-warning-dark border-warning',
  USER_CREATE: 'bg-success-lighter text-success-dark border-success',
  USER_UPDATE: 'bg-warning-lighter text-warning-dark border-warning',
  USER_DELETE: 'bg-error-lighter text-error-dark border-error',
  USER_DEACTIVATE: 'bg-warning-lighter text-warning-dark border-warning',
  ROLE_ASSIGN: 'bg-info-lighter text-info-dark border-info',
  ROLE_REVOKE: 'bg-warning-lighter text-warning-dark border-warning',
  SYSTEM_START: 'bg-neutral-200 text-neutral-700 border-neutral-400',
  SECURITY_ALERT: 'bg-error-lighter text-error-dark border-error',
};

const defaultColor = 'bg-neutral-200 text-neutral-700 border-neutral-400';

function formatTimestamp(timestamp: string): string {
  const date = new Date(timestamp);
  return date.toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  });
}

export function UserAuditModal({
  isOpen,
  onClose,
  user,
}: UserAuditModalProps) {
  const modalRef = useRef<HTMLDivElement>(null);
  const [logs, setLogs] = useState<UserAuditLogEntry[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  const fetchLogs = useCallback(async (page: number) => {
    if (!user) return;

    setIsLoading(true);
    setError(null);

    try {
      const response = await api.get<PaginatedResponse<UserAuditLogEntry>>(
        `/audit-logs?userId=${user.id}&page=${page}&size=10`
      );
      // Handle both paginated response and direct array response
      if (Array.isArray(response)) {
        setLogs(response);
        setTotalElements(response.length);
        setTotalPages(1);
        setCurrentPage(0);
      } else {
        setLogs(response.content || []);
        setCurrentPage(response.page || 0);
        setTotalPages(response.totalPages || 0);
        setTotalElements(response.totalElements || 0);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load audit logs');
      setLogs([]);
    } finally {
      setIsLoading(false);
    }
  }, [user]);

  useEffect(() => {
    if (isOpen && user) {
      fetchLogs(0);
    } else {
      setLogs([]);
      setCurrentPage(0);
      setTotalPages(0);
      setTotalElements(0);
    }
  }, [isOpen, user, fetchLogs]);

  useEffect(() => {
    if (isOpen) {
      const handleEscape = (e: KeyboardEvent) => {
        if (e.key === 'Escape') {
          onClose();
        }
      };
      document.addEventListener('keydown', handleEscape);
      return () => document.removeEventListener('keydown', handleEscape);
    }
  }, [isOpen, onClose]);

  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
      modalRef.current?.focus();
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [isOpen]);

  if (!isOpen || !user) return null;

  return (
    // eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
      role="dialog"
      aria-modal="true"
      aria-labelledby="audit-modal-title"
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
      onKeyDown={(e) => {
        if (e.key === 'Escape') onClose();
      }}
    >
      <div
        ref={modalRef}
        tabIndex={-1}
        className="w-full max-w-3xl max-h-[80vh] rounded-lg bg-white shadow-xl flex flex-col"
      >
        <div className="p-6 border-b border-neutral-200 flex items-center justify-between">
          <div>
            <h2 id="audit-modal-title" className="text-lg font-semibold text-neutral-900">
              Audit Logs
            </h2>
            <p className="text-sm text-neutral-500">
              Activity history for {user.fullName}
            </p>
          </div>
          <button
            onClick={onClose}
            className="rounded p-1 text-neutral-500 hover:bg-neutral-100 hover:text-neutral-700 focus-visible:outline-none focus-visible:ring-focus focus-visible:ring-primary focus-visible:ring-offset-focus"
            aria-label="Close modal"
          >
            <svg
              className="h-5 w-5"
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              strokeWidth={1.5}
              stroke="currentColor"
            >
              <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <div className="flex-1 overflow-auto p-6">
          {error && (
            <div className="mb-4 rounded border border-error bg-error-lighter p-4 text-error-dark" role="alert">
              {error}
            </div>
          )}

          <div aria-live="polite" className="sr-only">
            {isLoading
              ? 'Loading audit logs...'
              : `Showing ${logs?.length ?? 0} of ${totalElements} audit log entries`}
          </div>

          {isLoading && (!logs || logs.length === 0) ? (
            <div className="py-12 text-center">
              <div
                className="mx-auto h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent"
                role="status"
                aria-label="Loading audit logs"
              />
              <p className="mt-2 text-neutral-500">Loading audit logs...</p>
            </div>
          ) : !logs || logs.length === 0 ? (
            <div className="py-12 text-center text-neutral-500">
              No audit log entries found for this user
            </div>
          ) : (
            <ul className="space-y-2" role="log" aria-label="User audit log entries">
              {(logs || []).map((log) => (
                <li key={log.id}>
                  <article className="rounded-lg border border-neutral-200 p-4 flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                    <div className="flex-1 space-y-1">
                      <div className="flex flex-wrap items-center gap-2">
                        <span
                          className={cn(
                            'inline-block rounded border px-2 py-0.5 text-xs font-medium',
                            actionTypeColors[log.actionType] || defaultColor
                          )}
                        >
                          {actionTypeLabels[log.actionType] || log.actionType.replace(/_/g, ' ')}
                        </span>
                        <time
                          dateTime={log.timestamp}
                          className="text-sm text-neutral-500"
                        >
                          {formatTimestamp(log.timestamp)}
                        </time>
                      </div>

                      {(log.resourceName || log.resourceId) && (
                        <p className="text-sm text-neutral-900">
                          Resource:{' '}
                          <span className="font-medium">{log.resourceName || log.resourceId}</span>
                          {log.resourceType && (
                            <span className="text-neutral-500"> ({log.resourceType})</span>
                          )}
                        </p>
                      )}

                      {log.details && (
                        <p className="text-sm text-neutral-600">{log.details}</p>
                      )}
                    </div>

                    <div className="text-right text-xs text-neutral-500 shrink-0">
                      <p>IP: {log.ipAddress}</p>
                      <p>ID: {log.id.substring(0, 8)}...</p>
                    </div>
                  </article>
                </li>
              ))}
            </ul>
          )}

          {isLoading && logs && logs.length > 0 && (
            <div className="pt-4 text-center text-neutral-500">
              Loading...
            </div>
          )}
        </div>

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="p-6 border-t border-neutral-200">
            <nav aria-label="Audit log pagination" className="flex items-center justify-between">
              <p className="text-sm text-neutral-600">
                Page {currentPage + 1} of {totalPages} ({totalElements} total entries)
              </p>
              <div className="flex gap-1">
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => fetchLogs(currentPage - 1)}
                  disabled={currentPage === 0 || isLoading}
                  aria-label="Go to previous page"
                >
                  Previous
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  onClick={() => fetchLogs(currentPage + 1)}
                  disabled={currentPage >= totalPages - 1 || isLoading}
                  aria-label="Go to next page"
                >
                  Next
                </Button>
              </div>
            </nav>
          </div>
        )}

        <div className="p-6 border-t border-neutral-200">
          <div className="flex justify-end">
            <Button variant="secondary" onClick={onClose}>
              Close
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
