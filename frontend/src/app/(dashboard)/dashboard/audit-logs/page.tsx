'use client';

import { useCallback, useEffect, useState } from 'react';
import { useRequireAuth } from '@/lib/auth';
import { api } from '@/lib/api';
import {
  AuditLogStream,
  type AuditLogEntry,
  type AuditLogFilters,
} from '@/components/audit';

// Backend returns a plain array of audit logs

export default function AuditLogsPage() {
  const { isAuthorized } = useRequireAuth('AUDITOR', 'ADMINISTRATOR');
  const [logs, setLogs] = useState<AuditLogEntry[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [hasMore, setHasMore] = useState(false);
  const [cursor, setCursor] = useState<string | undefined>();
  const [filters, setFilters] = useState<AuditLogFilters>({});

  const loadLogs = useCallback(
    async (append: boolean, currentCursor?: string) => {
      setIsLoading(true);
      try {
        const params = new URLSearchParams();
        if (append && currentCursor) {
          params.set('cursor', currentCursor);
        }
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
        const endpoint = `/audit-logs${queryString ? `?${queryString}` : ''}`;
        const data = await api.get<AuditLogEntry[]>(endpoint);

        if (append) {
          setLogs((prev) => [...prev, ...data]);
        } else {
          setLogs(data);
        }
        // Backend doesn't support pagination yet
        setHasMore(false);
        setCursor(undefined);
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
    setCursor(undefined);
    setFilters(newFilters);
  };

  const handleLoadMore = () => {
    loadLogs(true, cursor);
  };

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
