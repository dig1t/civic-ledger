'use client';

import { useCallback, useEffect, useState } from 'react';
import { useRequireAuth } from '@/util/auth';
import { api } from '@/util/api';
import {
  AuditLogStream,
  type AuditLogEntry,
  type AuditLogFilters,
} from '@/features/audit';
import type { PaginatedResponse } from '@/types/api';

const PAGE_SIZE = 50;

export default function AuditLogsPage() {
  // Role check - redirects to /unauthorized if not AUDITOR or ADMINISTRATOR
  const { isAuthorized } = useRequireAuth('AUDITOR', 'ADMINISTRATOR');
  const [logs, setLogs] = useState<AuditLogEntry[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [filters, setFilters] = useState<AuditLogFilters>({});

  const loadLogs = useCallback(
    async (page: number) => {
      setIsLoading(true);
      try {
        const params = new URLSearchParams();
        params.set('page', page.toString());
        params.set('size', PAGE_SIZE.toString());
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

        const endpoint = `/audit-logs?${params.toString()}`;
        const response = await api.get<PaginatedResponse<AuditLogEntry>>(endpoint);

        setLogs(response.content || []);
        setCurrentPage(response.page);
        setTotalPages(response.totalPages);
        setTotalElements(response.totalElements);
      } catch {
        setLogs([]);
        setTotalPages(0);
        setTotalElements(0);
      } finally {
        setIsLoading(false);
      }
    },
    [filters]
  );

  useEffect(() => {
    if (isAuthorized) {
      loadLogs(0);
    }
  }, [isAuthorized, loadLogs]);

  const handleFilter = (newFilters: AuditLogFilters) => {
    setFilters(newFilters);
    // loadLogs will be called via useEffect due to filters dependency
  };

  const handlePageChange = (page: number) => {
    loadLogs(page);
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
        currentPage={currentPage}
        totalPages={totalPages}
        totalElements={totalElements}
        pageSize={PAGE_SIZE}
        onPageChange={handlePageChange}
        onFilter={handleFilter}
      />
    </div>
  );
}
