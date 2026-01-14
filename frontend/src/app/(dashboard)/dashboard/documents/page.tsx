'use client';

import { useCallback, useEffect, useState } from 'react';
import { useAuth, useRequireAuth } from '@/util/auth';
import { api } from '@/util/api';
import { Button, useToast } from '@/components';
import { DocumentList, FileUpload, type Document } from '@/features/documents';
import type { PaginatedResponse } from '@/types/api';

const PAGE_SIZE = 20;

export default function DocumentsPage() {
  // Role check - redirects to /unauthorized if not OFFICER or ADMINISTRATOR
  const { isAuthorized } = useRequireAuth('OFFICER', 'ADMINISTRATOR');
  const { hasRole } = useAuth();
  const { showToast } = useToast();
  const [documents, setDocuments] = useState<Document[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);

  const canUpload = hasRole('OFFICER', 'ADMINISTRATOR');

  const loadDocuments = useCallback(async (page: number, query?: string) => {
    setIsLoading(true);
    setError(null);
    try {
      let endpoint = `/documents?page=${page}&size=${PAGE_SIZE}`;
      if (query) {
        endpoint += `&search=${encodeURIComponent(query)}`;
      }
      const response = await api.get<PaginatedResponse<Document>>(endpoint);
      setDocuments(response.content || []);
      setCurrentPage(response.page ?? 0);
      setTotalPages(response.totalPages ?? 0);
      setTotalElements(response.totalElements ?? 0);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load documents');
      setDocuments([]);
      setTotalPages(0);
      setTotalElements(0);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (isAuthorized) {
      loadDocuments(0);
    }
  }, [isAuthorized, loadDocuments]);

  const handlePageChange = (page: number) => {
    loadDocuments(page, searchQuery);
  };

  const handleSearch = (query: string) => {
    setSearchQuery(query);
    loadDocuments(0, query);
  };

  async function handleDownload(doc: Document) {
    try {
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL}/documents/${doc.id}/download`,
        {
          headers: {
            Authorization: `Bearer ${localStorage.getItem('token')}`,
          },
        }
      );

      if (!response.ok) throw new Error('Download failed');

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = doc.fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error('Download failed:', error);
    }
  }

  async function handleUpload(file: File) {
    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch(
      `${process.env.NEXT_PUBLIC_API_URL}/documents/upload`,
      {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${localStorage.getItem('token')}`,
        },
        body: formData,
      }
    );

    if (!response.ok) {
      const data = await response.json().catch(() => ({}));
      throw new Error(data.message || 'Upload failed');
    }

    showToast('Document uploaded successfully', 'success');
    setIsUploadModalOpen(false);
    loadDocuments(0, searchQuery);
  }

  // Role check in progress or unauthorized - useRequireAuth handles redirect
  if (!isAuthorized) {
    return null;
  }

  return (
    <div className="space-y-6">
      <header className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-neutral-900">Documents</h1>
          <p className="mt-1 text-neutral-600">
            Search and download documents from the secure repository.
          </p>
        </div>
        {canUpload && (
          <Button onClick={() => setIsUploadModalOpen(true)}>
            Upload Document
          </Button>
        )}
      </header>

      {error && (
        <div className="rounded border border-error bg-error-lighter p-4 text-error-dark" role="alert">
          {error}
        </div>
      )}

      <DocumentList
        documents={documents}
        isLoading={isLoading}
        currentPage={currentPage}
        totalPages={totalPages}
        totalElements={totalElements}
        pageSize={PAGE_SIZE}
        onPageChange={handlePageChange}
        onSearch={handleSearch}
        onDownload={handleDownload}
      />

      {/* Upload Modal */}
      {isUploadModalOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50"
          role="dialog"
          aria-modal="true"
          aria-labelledby="upload-dialog-title"
        >
          <div className="w-full max-w-lg rounded-lg bg-white p-6 shadow-xl">
            <div className="mb-4 flex items-center justify-between">
              <h2 id="upload-dialog-title" className="text-lg font-semibold text-neutral-900">
                Upload Document
              </h2>
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setIsUploadModalOpen(false)}
                aria-label="Close upload dialog"
              >
                Close
              </Button>
            </div>
            <FileUpload onUpload={handleUpload} />
          </div>
        </div>
      )}
    </div>
  );
}
