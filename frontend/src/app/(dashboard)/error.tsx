'use client';

import { useEffect } from 'react';

export default function DashboardError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error('Dashboard error:', error);
  }, [error]);

  return (
    <div className="min-h-[400px] flex items-center justify-center">
      <div className="max-w-md w-full text-center p-6">
        <h2 className="text-xl font-bold text-neutral-900 mb-4">
          Something went wrong
        </h2>
        <p className="text-neutral-600 mb-6">
          {error.message || 'An error occurred while loading this page.'}
        </p>
        <button
          onClick={() => reset()}
          className="btn-primary"
        >
          Try again
        </button>
      </div>
    </div>
  );
}
