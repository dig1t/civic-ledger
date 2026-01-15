'use client';

import { useState } from 'react';
import { cn } from '@/util/utils';
import { FileUpload } from './file-upload';

export type ClassificationLevel = 'UNCLASSIFIED' | 'CONFIDENTIAL' | 'SECRET' | 'TOP_SECRET';

const classificationColors: Record<ClassificationLevel, string> = {
  UNCLASSIFIED: 'bg-success-lighter text-success-dark border-success',
  CONFIDENTIAL: 'bg-info-lighter text-info-dark border-info',
  SECRET: 'bg-warning-lighter text-warning-dark border-warning',
  TOP_SECRET: 'bg-error-lighter text-error-dark border-error',
};

interface UploadFormProps {
  onUpload: (file: File, classification: ClassificationLevel) => Promise<void>;
  accept?: string;
  maxSize?: number;
  maxFiles?: number;
}

export function UploadForm({
  onUpload,
  accept = '*',
  maxSize = 100 * 1024 * 1024,
  maxFiles = 10,
}: UploadFormProps) {
  const [classification, setClassification] = useState<ClassificationLevel>('UNCLASSIFIED');

  async function handleUpload(file: File) {
    await onUpload(file, classification);
  }

  return (
    <div className="space-y-4">
      {/* Classification selection */}
      <div>
        <label
          htmlFor="classification"
          className="block text-sm font-medium text-neutral-900"
        >
          Classification Level <span className="text-error">*</span>
        </label>
        <div className="mt-1 flex items-center gap-3">
          <select
            id="classification"
            value={classification}
            onChange={(e) => setClassification(e.target.value as ClassificationLevel)}
            className="min-h-touch flex-1 rounded border-2 border-neutral-400 bg-white px-3 py-2 text-neutral-900 focus:border-primary focus:outline-none focus:ring-focus focus:ring-primary focus:ring-offset-focus"
          >
            <option value="UNCLASSIFIED">UNCLASSIFIED</option>
            <option value="CONFIDENTIAL">CONFIDENTIAL</option>
            <option value="SECRET">SECRET</option>
            <option value="TOP_SECRET">TOP SECRET</option>
          </select>
          <span
            className={cn(
              'inline-block rounded border px-3 py-2 text-xs font-semibold',
              classificationColors[classification]
            )}
          >
            {classification.replace('_', ' ')}
          </span>
        </div>
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
          accept={accept}
          maxSize={maxSize}
          maxFiles={maxFiles}
        />
      </div>

      {/* Security notice */}
      <div className="rounded-lg bg-blue-50 p-4 text-sm">
        <h3 className="font-semibold text-neutral-900">Security Notice</h3>
        <ul className="mt-2 list-inside list-disc space-y-1 text-neutral-700">
          <li>All uploaded files are encrypted with AES-256-GCM</li>
          <li>SHA-256 hash is calculated to ensure file integrity</li>
          <li>Upload action is recorded in the immutable audit log</li>
          <li>Access is restricted based on your role and clearance level</li>
        </ul>
      </div>
    </div>
  );
}
