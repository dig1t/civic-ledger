'use client';

import { useCallback, useEffect, useState } from 'react';
import { useRequireAuth } from '@/util/auth';
import { api } from '@/util/api';
import {
  AuditLogStream,
  type AuditLogEntry,
  type AuditLogFilters,
} from '@/features/audit';

interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export default function AuditLogsPage() {
  // Role check - redirects to /unauthorized if not AUDITOR or ADMINISTRATOR
  const { isAuthorized } = useRequireAuth('AUDITOR', 'ADMINISTRATOR');
  const [logs, setLogs] = useState<AuditLogEntry[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [hasMore, setHasMore] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);
  const [filters, setFilters] = useState<AuditLogFilters>({});

  const loadLogs = useCallback(
    async (append: boolean, page: number = 0) => {
      setIsLoading(true);
      try {
        const params = new URLSearchParams();
        params.set('page', page.toString());
        params.set('size', '50');
        if (filters.actionType) {
          params.set('actionType', filters.actionType);
        }
        if (filters.userId) {
          params.set('userId', filters.userId);
        }
        if (filters.startDate) {
          params.set('startDate', filters.startDate);
        }
        if (filters.endDate) {
          params.set('endDate', filters.endDate);
        }

        const queryString = params.toString();
        const endpoint = `/audit-logs?${queryString}`;
        const data = await api.get<PaginatedResponse<AuditLogEntry>>(endpoint);

        if (append) {
          setLogs((prev) => [...prev, ...(data.content || [])]);
        } else {
          setLogs(data.content || []);
        }
        setCurrentPage(data.page);
        setHasMore(!data.last);
      } catch {
        if (!append) {
          setLogs([]);
        }
        setHasMore(false);
      } finally {
        setIsLoading(false);
      }
    },
    [filters]
  );

  useEffect(() => {
    if (isAuthorized) {
      loadLogs(false);
    }
  }, [isAuthorized, loadLogs]);

  const handleFilter = (newFilters: AuditLogFilters) => {
    setCurrentPage(0);
    setFilters(newFilters);
  };

  const handleLoadMore = () => {
    loadLogs(true, currentPage + 1);
  };

  // Role check in progress or unauthorized - useRequireAuth handles redirect
  if (!isAuthorized) {
    return null;
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold text-neutral-900">Audit Logs</h1>
        <p className="mt-1 text-neutral-600">
          Chronological stream of all system events. Logs are immutable and
          cannot be modified or deleted.
        </p>
      </header>

      <div className="alert-info">
        <h2 className="font-semibold text-neutral-900">Audit Trail Notice</h2>
        <p className="mt-1 text-sm">
          This audit log provides a complete chain of custody for all documents
          and system actions. All entries are cryptographically sealed and
          comply with NIST 800-53 AU-2 (Audit Events) requirements.
        </p>
      </div>

      <AuditLogStream
        logs={logs}
        isLoading={isLoading}
        hasMore={hasMore}
        onLoadMore={handleLoadMore}
        onFilter={handleFilter}
      />
    </div>
  );
}
