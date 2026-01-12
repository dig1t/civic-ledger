'use client';

import { useState } from 'react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

export interface Document {
  id: string;
  fileName: string;
  fileHash: string;
  classificationLevel: 'UNCLASSIFIED' | 'CONFIDENTIAL' | 'SECRET' | 'TOP_SECRET';
  versionNumber: number;
  uploadedBy: string;
  uploadedAt: string;
  fileSize: number;
}

interface DocumentListProps {
  documents: Document[];
  isLoading?: boolean;
  onDownload?: (doc: Document) => void;
  onSearch?: (query: string) => void;
}

const classificationColors = {
  UNCLASSIFIED: 'bg-success-lighter text-success-dark border-success',
  CONFIDENTIAL: 'bg-info-lighter text-info-dark border-info',
  SECRET: 'bg-warning-lighter text-warning-dark border-warning',
  TOP_SECRET: 'bg-error-lighter text-error-dark border-error',
};

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function DocumentList({
  documents,
  isLoading,
  onDownload,
  onSearch,
}: DocumentListProps) {
  const [searchQuery, setSearchQuery] = useState('');

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    onSearch?.(searchQuery);
  };

  return (
    <div className="space-y-4">
      {/* Search form */}
      <form onSubmit={handleSearch} className="flex gap-2" role="search">
        <label htmlFor="document-search" className="sr-only">
          Search documents
        </label>
        <Input
          id="document-search"
          type="search"
          placeholder="Search by file name or hash..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="flex-1"
        />
        <Button type="submit" variant="secondary">
          Search
        </Button>
      </form>

      {/* Results count for screen readers */}
      <div aria-live="polite" className="sr-only">
        {isLoading
          ? 'Loading documents...'
          : `${documents.length} documents found`}
      </div>

      {/* Document table */}
      <div className="overflow-x-auto rounded-lg border border-neutral-200">
        <table className="w-full text-left">
          <caption className="sr-only">List of documents</caption>
          <thead className="border-b border-neutral-200 bg-neutral-50">
            <tr>
              <th scope="col" className="px-4 py-3 text-sm font-semibold text-neutral-900">
                File Name
              </th>
              <th scope="col" className="px-4 py-3 text-sm font-semibold text-neutral-900">
                Classification
              </th>
              <th scope="col" className="px-4 py-3 text-sm font-semibold text-neutral-900">
                Version
              </th>
              <th scope="col" className="px-4 py-3 text-sm font-semibold text-neutral-900">
                Size
              </th>
              <th scope="col" className="px-4 py-3 text-sm font-semibold text-neutral-900">
                Uploaded
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
                    <span className="text-neutral-500">Loading documents...</span>
                  </div>
                </td>
              </tr>
            ) : documents.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-neutral-500">
                  No documents found
                </td>
              </tr>
            ) : (
              documents.map((doc) => (
                <tr key={doc.id} className="hover:bg-neutral-50">
                  <td className="px-4 py-3">
                    <div>
                      <p className="font-medium text-neutral-900">{doc.fileName}</p>
                      <p className="truncate text-xs text-neutral-500" title={doc.fileHash}>
                        SHA-256: {doc.fileHash.substring(0, 16)}...
                      </p>
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <span
                      className={cn(
                        'inline-block rounded border px-2 py-1 text-xs font-medium',
                        classificationColors[doc.classificationLevel]
                      )}
                    >
                      {doc.classificationLevel.replace('_', ' ')}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm text-neutral-700">v{doc.versionNumber}</td>
                  <td className="px-4 py-3 text-sm text-neutral-700">{formatFileSize(doc.fileSize)}</td>
                  <td className="px-4 py-3">
                    <div>
                      <p className="text-sm text-neutral-700">{formatDate(doc.uploadedAt)}</p>
                      <p className="text-xs text-neutral-500">by {doc.uploadedBy}</p>
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => onDownload?.(doc)}
                      aria-label={`Download ${doc.fileName}`}
                    >
                      Download
                    </Button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
