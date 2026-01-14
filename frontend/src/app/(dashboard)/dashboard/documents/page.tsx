'use client';

import { useEffect, useState } from 'react';
import { useRequireAuth } from '@/util/auth';
import { api } from '@/util/api';
import { DocumentList, type Document } from '@/features/documents';

export default function DocumentsPage() {
  // Role check - redirects to /unauthorized if not OFFICER or ADMINISTRATOR
  const { isAuthorized } = useRequireAuth('OFFICER', 'ADMINISTRATOR');
  const [documents, setDocuments] = useState<Document[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    if (isAuthorized) {
      loadDocuments();
    }
  }, [isAuthorized]);

  async function loadDocuments(query?: string) {
    setIsLoading(true);
    try {
      const endpoint = query
        ? `/documents?search=${encodeURIComponent(query)}`
        : '/documents';
      const data = await api.get<Document[]>(endpoint);
      setDocuments(data);
    } catch {
      setDocuments([]);
    } finally {
      setIsLoading(false);
    }
  }

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
        onSearch={loadDocuments}
        onDownload={handleDownload}
      />
    </div>
  );
}
