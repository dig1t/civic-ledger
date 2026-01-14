'use client';

import { useCallback, useEffect, useState } from 'react';
import { useRequireAuth } from '@/util/auth';
import { api } from '@/util/api';
import { DocumentList, type Document } from '@/features/documents';
import type { PaginatedResponse } from '@/types/api';

const PAGE_SIZE = 20;

export default function DocumentsPage() {
  // Role check - redirects to /unauthorized if not OFFICER or ADMINISTRATOR
  const { isAuthorized } = useRequireAuth('OFFICER', 'ADMINISTRATOR');
  const [documents, setDocuments] = useState<Document[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [searchQuery, setSearchQuery] = useState('');

  const loadDocuments = useCallback(async (page: number, query?: string) => {
    setIsLoading(true);
    try {
      let endpoint = `/documents?page=${page}&size=${PAGE_SIZE}`;
      if (query) {
        endpoint += `&search=${encodeURIComponent(query)}`;
      }
      const response = await api.get<PaginatedResponse<Document>>(endpoint);
      setDocuments(response.content);
      setCurrentPage(response.page);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch {
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

  // Role check in progress or unauthorized - useRequireAuth handles redirect
  if (!isAuthorized) {
    return null;
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold text-neutral-900">Documents</h1>
        <p className="mt-1 text-neutral-600">
          Search and download documents from the secure repository.
        </p>
      </header>

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
    </div>
  );
}
