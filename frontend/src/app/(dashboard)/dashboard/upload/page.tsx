'use client';

import { useState } from 'react';
import { useRequireAuth } from '@/lib/auth';
import { api } from '@/lib/api';
import { FileUpload } from '@/components/documents';
import { FormField } from '@/components/ui';

type ClassificationLevel = 'UNCLASSIFIED' | 'CONFIDENTIAL' | 'SECRET' | 'TOP_SECRET';

export default function UploadPage() {
  const { isAuthorized } = useRequireAuth('OFFICER', 'ADMINISTRATOR');
  const [classification, setClassification] = useState<ClassificationLevel>('UNCLASSIFIED');
  const [uploadedFile, setUploadedFile] = useState<File | null>(null);

  async function handleUpload(file: File) {
    setUploadedFile(file);

    await api.upload('/documents/upload', file, (progress) => {
      console.log(`Upload progress: ${progress}%`);
    });
  }

  if (!isAuthorized) {
    return null;
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-bold text-neutral-900">Upload Document</h1>
        <p className="mt-1 text-neutral-600">
          Upload a new document to the secure repository. All files are
          encrypted with AES-256-GCM before storage.
        </p>
      </header>

      <div className="card max-w-2xl space-y-6">
        {/* Classification selection */}
        <div>
          <label
            htmlFor="classification"
            className="block text-sm font-medium text-neutral-900"
          >
            Classification Level <span className="text-error">*</span>
          </label>
          <select
            id="classification"
            value={classification}
            onChange={(e) =>
              setClassification(e.target.value as ClassificationLevel)
            }
            className="mt-1 min-h-touch w-full rounded border-2 border-neutral-400 bg-white px-3 py-2 text-neutral-900 focus:border-primary focus:outline-none focus:ring-focus focus:ring-primary focus:ring-offset-focus"
          >
            <option value="UNCLASSIFIED">UNCLASSIFIED</option>
            <option value="CONFIDENTIAL">CONFIDENTIAL</option>
            <option value="SECRET">SECRET</option>
            <option value="TOP_SECRET">TOP SECRET</option>
          </select>
          <p className="mt-1 text-sm text-neutral-500">
            Select the appropriate classification level for this document.
          </p>
        </div>

        {/* File upload */}
        <div>
          <p className="mb-2 text-sm font-medium text-neutral-900">
            Document File <span className="text-error">*</span>
          </p>
          <FileUpload
            onUpload={handleUpload}
            accept=".pdf,.doc,.docx,.txt,.xls,.xlsx"
            maxSize={100 * 1024 * 1024} // 100MB
          />
        </div>

        {/* Security notice */}
        <div className="alert-info">
          <h3 className="font-semibold text-neutral-900">Security Notice</h3>
          <ul className="mt-2 list-inside list-disc space-y-1 text-sm">
            <li>All uploaded files are encrypted with AES-256-GCM</li>
            <li>SHA-256 hash is calculated to ensure file integrity</li>
            <li>Upload action is recorded in the immutable audit log</li>
            <li>Access is restricted based on your role and clearance level</li>
          </ul>
        </div>
      </div>
    </div>
  );
}
