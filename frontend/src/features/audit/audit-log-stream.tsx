'use client';

import { useEffect, useRef, useState } from 'react';
import { cn } from '@/util/utils';
import { Button, Input } from '@/components';

export interface AuditLogEntry {
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

interface AuditLogStreamProps {
  logs: AuditLogEntry[];
  isLoading?: boolean;
  hasMore?: boolean;
  onLoadMore?: () => void;
  onFilter?: (filters: AuditLogFilters) => void;
}

export interface AuditLogFilters {
  actionType?: string;
  userId?: string;
  startDate?: string;
  endDate?: string;
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

export function AuditLogStream({
  logs,
  isLoading,
  hasMore,
  onLoadMore,
  onFilter,
}: AuditLogStreamProps) {
  const [filters, setFilters] = useState<AuditLogFilters>({});
  const [isFiltersOpen, setIsFiltersOpen] = useState(false);
  const liveRegionRef = useRef<HTMLDivElement>(null);
  const [newLogsCount, setNewLogsCount] = useState(0);

  // Announce new logs to screen readers
  useEffect(() => {
    const count = logs?.length ?? 0;
    if (count > 0 && newLogsCount !== count) {
      setNewLogsCount(count);
    }
  }, [logs, newLogsCount]);

  const handleFilterSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onFilter?.(filters);
  };

  const handleFilterChange = (key: keyof AuditLogFilters, value: string) => {
    setFilters((prev) => ({ ...prev, [key]: value || undefined }));
  };

  const clearFilters = () => {
    setFilters({});
    onFilter?.({});
  };

  return (
    <div className="space-y-4">
      {/* Live region for screen reader announcements */}
      <div
        ref={liveRegionRef}
        aria-live="polite"
        aria-atomic="false"
        className="sr-only"
      >
        {isLoading
          ? 'Loading audit logs...'
          : `Showing ${logs?.length ?? 0} audit log entries`}
      </div>

      {/* Filters toggle */}
      <div className="flex items-center justify-between">
        <Button
          variant="secondary"
          onClick={() => setIsFiltersOpen(!isFiltersOpen)}
          aria-expanded={isFiltersOpen}
          aria-controls="audit-filters"
        >
          {isFiltersOpen ? 'Hide Filters' : 'Show Filters'}
        </Button>

        {Object.values(filters).some(Boolean) && (
          <Button variant="ghost" onClick={clearFilters}>
            Clear Filters
          </Button>
        )}
      </div>

      {/* Filters panel */}
      {isFiltersOpen && (
        <form
          id="audit-filters"
          onSubmit={handleFilterSubmit}
          className="card space-y-4"
        >
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div>
              <label
                htmlFor="filter-action"
                className="block text-sm font-medium text-neutral-900"
              >
                Action Type
              </label>
              <select
                id="filter-action"
                value={filters.actionType || ''}
                onChange={(e) => handleFilterChange('actionType', e.target.value)}
                className="mt-1 min-h-touch w-full rounded border-2 border-neutral-400 bg-white px-3 py-2 text-neutral-900 focus:border-primary focus:outline-none focus:ring-focus focus:ring-primary focus:ring-offset-focus"
              >
                <option value="">All Actions</option>
                {Object.entries(actionTypeLabels).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label
                htmlFor="filter-user"
                className="block text-sm font-medium text-neutral-900"
              >
                User ID
              </label>
              <Input
                id="filter-user"
                type="text"
                placeholder="Enter user ID"
                value={filters.userId || ''}
                onChange={(e) => handleFilterChange('userId', e.target.value)}
                className="mt-1"
              />
            </div>

            <div>
              <label
                htmlFor="filter-start"
                className="block text-sm font-medium text-neutral-900"
              >
                Start Date
              </label>
              <Input
                id="filter-start"
                type="date"
                value={filters.startDate || ''}
                onChange={(e) => handleFilterChange('startDate', e.target.value)}
                className="mt-1"
              />
            </div>

            <div>
              <label
                htmlFor="filter-end"
                className="block text-sm font-medium text-neutral-900"
              >
                End Date
              </label>
              <Input
                id="filter-end"
                type="date"
                value={filters.endDate || ''}
                onChange={(e) => handleFilterChange('endDate', e.target.value)}
                className="mt-1"
              />
            </div>
          </div>

          <div className="flex justify-end">
            <Button type="submit">Apply Filters</Button>
          </div>
        </form>
      )}

      {/* Audit log list */}
      <div className="space-y-2">
        {isLoading && (!logs || logs.length === 0) ? (
          <div className="card py-12 text-center">
            <div
              className="mx-auto h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent"
              role="status"
              aria-label="Loading audit logs"
            />
            <p className="mt-2 text-neutral-500">Loading audit logs...</p>
          </div>
        ) : !logs || logs.length === 0 ? (
          <div className="card py-12 text-center text-neutral-500">
            No audit log entries found
          </div>
        ) : (
          <ul className="space-y-2" role="log" aria-label="Audit log entries">
            {logs.map((log) => (
              <li key={log.id}>
                <article className="card flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
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

                    <p className="text-sm text-neutral-900">
                      <span className="font-medium">{log.userName || log.userId}</span>
                      {(log.resourceName || log.resourceId) && (
                        <>
                          {' '}
                          &rarr;{' '}
                          <span className="font-medium">{log.resourceName || log.resourceId}</span>
                        </>
                      )}
                    </p>

                    {log.details && (
                      <p className="text-sm text-neutral-600">{log.details}</p>
                    )}
                  </div>

                  <div className="text-right text-xs text-neutral-500">
                    <p>IP: {log.ipAddress}</p>
                    <p>ID: {log.id.substring(0, 8)}...</p>
                  </div>
                </article>
              </li>
            ))}
          </ul>
        )}

        {/* Load more button */}
        {hasMore && !isLoading && (
          <div className="pt-4 text-center">
            <Button variant="secondary" onClick={onLoadMore}>
              Load More
            </Button>
          </div>
        )}

        {isLoading && logs && logs.length > 0 && (
          <div className="pt-4 text-center text-neutral-500">
            Loading more entries...
          </div>
        )}
      </div>
    </div>
  );
}
