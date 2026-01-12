'use client';

import { useEffect, useRef, useState } from 'react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

export interface AuditLogEntry {
  id: string;
  actionType:
    | 'DOCUMENT_UPLOAD'
    | 'DOCUMENT_DOWNLOAD'
    | 'DOCUMENT_DELETE'
    | 'USER_LOGIN'
    | 'USER_LOGOUT'
    | 'USER_CREATE'
    | 'USER_UPDATE'
    | 'PERMISSION_CHANGE'
    | 'SYSTEM_EVENT';
  userId: string;
  userName: string;
  resourceId?: string;
  resourceName?: string;
  ipAddress: string;
  timestamp: string;
  details?: string;
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

const actionTypeLabels: Record<AuditLogEntry['actionType'], string> = {
  DOCUMENT_UPLOAD: 'Document Upload',
  DOCUMENT_DOWNLOAD: 'Document Download',
  DOCUMENT_DELETE: 'Document Delete',
  USER_LOGIN: 'User Login',
  USER_LOGOUT: 'User Logout',
  USER_CREATE: 'User Created',
  USER_UPDATE: 'User Updated',
  PERMISSION_CHANGE: 'Permission Change',
  SYSTEM_EVENT: 'System Event',
};

const actionTypeColors: Record<AuditLogEntry['actionType'], string> = {
  DOCUMENT_UPLOAD: 'bg-success-lighter text-success-dark border-success',
  DOCUMENT_DOWNLOAD: 'bg-info-lighter text-info-dark border-info',
  DOCUMENT_DELETE: 'bg-error-lighter text-error-dark border-error',
  USER_LOGIN: 'bg-primary-lighter text-primary-dark border-primary',
  USER_LOGOUT: 'bg-neutral-200 text-neutral-700 border-neutral-400',
  USER_CREATE: 'bg-success-lighter text-success-dark border-success',
  USER_UPDATE: 'bg-warning-lighter text-warning-dark border-warning',
  PERMISSION_CHANGE: 'bg-warning-lighter text-warning-dark border-warning',
  SYSTEM_EVENT: 'bg-neutral-200 text-neutral-700 border-neutral-400',
};

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
    if (logs.length > 0 && newLogsCount !== logs.length) {
      setNewLogsCount(logs.length);
    }
  }, [logs.length, newLogsCount]);

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
          : `Showing ${logs.length} audit log entries`}
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
        {isLoading && logs.length === 0 ? (
          <div className="card py-12 text-center">
            <div
              className="mx-auto h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent"
              role="status"
              aria-label="Loading audit logs"
            />
            <p className="mt-2 text-neutral-500">Loading audit logs...</p>
          </div>
        ) : logs.length === 0 ? (
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
                          actionTypeColors[log.actionType]
                        )}
                      >
                        {actionTypeLabels[log.actionType]}
                      </span>
                      <time
                        dateTime={log.timestamp}
                        className="text-sm text-neutral-500"
                      >
                        {formatTimestamp(log.timestamp)}
                      </time>
                    </div>

                    <p className="text-sm text-neutral-900">
                      <span className="font-medium">{log.userName}</span>
                      {log.resourceName && (
                        <>
                          {' '}
                          &rarr;{' '}
                          <span className="font-medium">{log.resourceName}</span>
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

        {isLoading && logs.length > 0 && (
          <div className="pt-4 text-center text-neutral-500">
            Loading more entries...
          </div>
        )}
      </div>
    </div>
  );
}
