'use client';

import { useEffect, useState } from 'react';
import { useAuth } from '@/util/auth';
import { api } from '@/util/api';

interface DashboardStats {
  totalDocuments: number;
  documentsThisMonth: number;
  auditLogsToday: number;
  activeUsers: number;
}

export default function DashboardPage() {
  const { user } = useAuth();
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    api
      .get<DashboardStats>('/dashboard/stats')
      .then(setStats)
      .catch(() => {
        setStats({
          totalDocuments: 0,
          documentsThisMonth: 0,
          auditLogsToday: 0,
          activeUsers: 0,
        });
      })
      .finally(() => setIsLoading(false));
  }, []);

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold text-neutral-900">
          Welcome, {user?.name}
        </h1>
        <p className="mt-1 text-neutral-600">
          Here&apos;s an overview of your secure document management system.
        </p>
      </header>

      {/* Stats grid */}
      <section aria-labelledby="stats-heading">
        <h2 id="stats-heading" className="sr-only">
          Dashboard Statistics
        </h2>

        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <StatCard
            label="Total Documents"
            value={stats?.totalDocuments ?? 0}
            isLoading={isLoading}
          />
          <StatCard
            label="Documents This Month"
            value={stats?.documentsThisMonth ?? 0}
            isLoading={isLoading}
          />
          <StatCard
            label="Audit Logs Today"
            value={stats?.auditLogsToday ?? 0}
            isLoading={isLoading}
          />
          {user?.role === 'ADMINISTRATOR' && (
            <StatCard
              label="Active Users"
              value={stats?.activeUsers ?? 0}
              isLoading={isLoading}
            />
          )}
        </div>
      </section>

      {/* Quick actions based on role */}
      <section aria-labelledby="actions-heading">
        <h2
          id="actions-heading"
          className="text-lg font-semibold text-neutral-900"
        >
          Quick Actions
        </h2>

        <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {(user?.role === 'OFFICER' || user?.role === 'ADMINISTRATOR') && (
            <>
              <ActionCard
                href="/dashboard/upload"
                title="Upload Document"
                description="Upload a new document to the secure repository"
              />
              <ActionCard
                href="/dashboard/documents"
                title="Browse Documents"
                description="Search and download existing documents"
              />
            </>
          )}

          {(user?.role === 'AUDITOR' || user?.role === 'ADMINISTRATOR') && (
            <ActionCard
              href="/dashboard/audit-logs"
              title="View Audit Logs"
              description="Review system activity and audit trail"
            />
          )}

          {user?.role === 'ADMINISTRATOR' && (
            <ActionCard
              href="/dashboard/users"
              title="Manage Users"
              description="Add, edit, or deactivate user accounts"
            />
          )}
        </div>
      </section>
    </div>
  );
}

function StatCard({
  label,
  value,
  isLoading,
}: {
  label: string;
  value: number;
  isLoading: boolean;
}) {
  return (
    <div className="card">
      <p className="text-sm font-medium text-neutral-500">{label}</p>
      {isLoading ? (
        <div className="mt-2 h-8 w-16 animate-pulse rounded bg-neutral-200" />
      ) : (
        <p className="mt-2 text-3xl font-bold text-neutral-900">{value}</p>
      )}
    </div>
  );
}

function ActionCard({
  href,
  title,
  description,
}: {
  href: string;
  title: string;
  description: string;
}) {
  return (
    <a
      href={href}
      className="card block transition-shadow hover:shadow-md focus-visible:outline-none focus-visible:ring-focus focus-visible:ring-primary focus-visible:ring-offset-focus"
    >
      <h3 className="font-semibold text-primary">{title}</h3>
      <p className="mt-1 text-sm text-neutral-600">{description}</p>
    </a>
  );
}
