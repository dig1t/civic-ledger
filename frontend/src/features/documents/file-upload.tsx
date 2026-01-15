'use client';

import { useCallback, useId, useRef, useState, type DragEvent } from 'react';
import { cn } from '@/util/utils';
import { Button } from '@/components';

export type UploadStatus = 'idle' | 'uploading' | 'success' | 'error';

interface FileUploadProps {
  onUpload: (file: File) => Promise<void>;
  accept?: string;
  maxSize?: number;
  maxFiles?: number;
  disabled?: boolean;
}

interface QueuedFile {
  id: string;
  file: File;
  status: 'pending' | 'uploading' | 'success' | 'error';
  error?: string;
}

export function FileUpload({
  onUpload,
  accept = '*',
  maxSize = 50 * 1024 * 1024, // 50MB default
  maxFiles = 10,
  disabled = false,
}: FileUploadProps) {
  const inputId = useId();
  const inputRef = useRef<HTMLInputElement>(null);
  const [queuedFiles, setQueuedFiles] = useState<QueuedFile[]>([]);
  const [isUploading, setIsUploading] = useState(false);
  const [isDragging, setIsDragging] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const validateFile = useCallback(
    (file: File): string | null => {
      if (file.size > maxSize) {
        return `File size exceeds maximum allowed (${Math.round(maxSize / 1024 / 1024)}MB)`;
      }

      if (accept !== '*') {
        const allowedExtensions = accept.split(',').map((ext) => ext.trim().toLowerCase());
        const fileExtension = '.' + file.name.split('.').pop()?.toLowerCase();
        if (!allowedExtensions.includes(fileExtension)) {
          return `File type not allowed. Accepted types: ${accept}`;
        }
      }

      return null;
    },
    [maxSize, accept]
  );

  const addFiles = useCallback(
    (files: FileList | File[]) => {
      const fileArray = Array.from(files);
      const currentCount = queuedFiles.length;
      const availableSlots = maxFiles - currentCount;

      if (availableSlots <= 0) {
        setError(`Maximum ${maxFiles} files allowed`);
        return;
      }

      const filesToAdd = fileArray.slice(0, availableSlots);
      const newQueuedFiles: QueuedFile[] = [];

      for (const file of filesToAdd) {
        // Check for duplicates
        const isDuplicate = queuedFiles.some((qf) => qf.file.name === file.name && qf.file.size === file.size);
        if (isDuplicate) {
          continue;
        }

        const validationError = validateFile(file);
        newQueuedFiles.push({
          id: `${file.name}-${file.size}-${Date.now()}`,
          file,
          status: validationError ? 'error' : 'pending',
          error: validationError || undefined,
        });
      }

      if (newQueuedFiles.length > 0) {
        setQueuedFiles((prev) => [...prev, ...newQueuedFiles]);
        setError(null);
      }

      if (fileArray.length > availableSlots) {
        setError(`Only ${availableSlots} more file(s) can be added (max ${maxFiles})`);
      }
    },
    [queuedFiles, maxFiles, validateFile]
  );

  const removeFile = useCallback((id: string) => {
    setQueuedFiles((prev) => prev.filter((f) => f.id !== id));
    setError(null);
  }, []);

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      addFiles(e.target.files);
    }
    // Reset input so same file can be selected again
    if (inputRef.current) {
      inputRef.current.value = '';
    }
  };

  const handleDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(false);

    if (disabled || isUploading) return;

    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      addFiles(e.dataTransfer.files);
    }
  };

  const handleDragOver = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    if (!disabled && !isUploading) {
      setIsDragging(true);
    }
  };

  const handleDragLeave = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleSubmit = async () => {
    const pendingFiles = queuedFiles.filter((f) => f.status === 'pending');
    if (pendingFiles.length === 0) return;

    setIsUploading(true);
    setError(null);

    for (const queuedFile of pendingFiles) {
      // Update status to uploading
      setQueuedFiles((prev) =>
        prev.map((f) => (f.id === queuedFile.id ? { ...f, status: 'uploading' as const } : f))
      );

      try {
        await onUpload(queuedFile.file);
        // Update status to success
        setQueuedFiles((prev) =>
          prev.map((f) => (f.id === queuedFile.id ? { ...f, status: 'success' as const } : f))
        );
      } catch (err) {
        // Update status to error
        setQueuedFiles((prev) =>
          prev.map((f) =>
            f.id === queuedFile.id
              ? { ...f, status: 'error' as const, error: err instanceof Error ? err.message : 'Upload failed' }
              : f
          )
        );
      }
    }

    setIsUploading(false);
  };

  const clearAll = () => {
    setQueuedFiles([]);
    setError(null);
    if (inputRef.current) {
      inputRef.current.value = '';
    }
  };

  const pendingCount = queuedFiles.filter((f) => f.status === 'pending').length;
  const successCount = queuedFiles.filter((f) => f.status === 'success').length;
  const errorCount = queuedFiles.filter((f) => f.status === 'error').length;

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  };

  return (
    <div className="space-y-4">
      {/* Visually hidden live region for screen readers */}
      <div aria-live="polite" aria-atomic="true" className="sr-only" role="status">
        {isUploading && 'Uploading files...'}
        {!isUploading && successCount > 0 && `${successCount} file(s) uploaded successfully`}
        {error && `Error: ${error}`}
      </div>

      {/* Drop zone */}
      {/* eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions */}
      <label
        htmlFor={inputId}
        className={cn(
          'relative flex cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed p-6 text-center transition-all',
          isDragging
            ? 'border-primary bg-primary-lighter'
            : 'border-neutral-300 bg-neutral-50 hover:border-primary hover:bg-primary-lighter/50',
          (disabled || isUploading) && 'cursor-not-allowed opacity-50'
        )}
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
      >
        <input
          ref={inputRef}
          id={inputId}
          type="file"
          accept={accept === '*' ? undefined : accept}
          multiple
          onChange={handleInputChange}
          disabled={disabled || isUploading || queuedFiles.length >= maxFiles}
          className="sr-only"
          aria-describedby={`${inputId}-description`}
        />

        <div className="flex items-center gap-3">
          <div className="h-6 w-6 text-primary" aria-hidden="true">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              strokeWidth={2}
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5"
              />
            </svg>
          </div>

          <p id={`${inputId}-label`} className="text-base font-medium text-neutral-900">
            <span className="text-primary underline">Click to select files</span> or drag and drop
          </p>
        </div>

        <p id={`${inputId}-description`} className="mt-2 text-sm text-neutral-500">
          {accept === '*' ? 'All file types accepted' : `Accepted formats: ${accept}`} (max{' '}
          {Math.round(maxSize / 1024 / 1024)}MB per file, up to {maxFiles} files)
        </p>
      </label>

      {/* Error message */}
      {error && (
        <div className="rounded-lg bg-error-lighter p-3 text-sm text-error-dark" role="alert">
          {error}
        </div>
      )}

      {/* File list */}
      {queuedFiles.length > 0 && (
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <p className="text-sm font-medium text-neutral-700">
              Selected files ({queuedFiles.length}/{maxFiles})
            </p>
            {!isUploading && (
              <Button variant="ghost" size="sm" onClick={clearAll}>
                Clear all
              </Button>
            )}
          </div>

          <ul className="divide-y divide-neutral-200 rounded-lg border border-neutral-200">
            {queuedFiles.map((qf) => (
              <li key={qf.id} className="flex items-center justify-between p-3">
                <div className="flex items-center gap-3 overflow-hidden">
                  {/* Status icon */}
                  {qf.status === 'pending' && (
                    <div className="h-5 w-5 rounded-full border-2 border-neutral-300" aria-hidden="true" />
                  )}
                  {qf.status === 'uploading' && (
                    <div className="h-5 w-5 animate-spin rounded-full border-2 border-primary border-t-transparent" aria-label="Uploading" />
                  )}
                  {qf.status === 'success' && (
                    <svg className="h-5 w-5 text-success" viewBox="0 0 20 20" fill="currentColor" aria-label="Uploaded">
                      <path fillRule="evenodd" d="M16.704 4.153a.75.75 0 01.143 1.052l-8 10.5a.75.75 0 01-1.127.075l-4.5-4.5a.75.75 0 011.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 011.05-.143z" clipRule="evenodd" />
                    </svg>
                  )}
                  {qf.status === 'error' && (
                    <svg className="h-5 w-5 text-error" viewBox="0 0 20 20" fill="currentColor" aria-label="Error">
                      <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-5a.75.75 0 01.75.75v4.5a.75.75 0 01-1.5 0v-4.5A.75.75 0 0110 5zm0 10a1 1 0 100-2 1 1 0 000 2z" clipRule="evenodd" />
                    </svg>
                  )}

                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-neutral-900">{qf.file.name}</p>
                    <p className="text-xs text-neutral-500">
                      {formatFileSize(qf.file.size)}
                      {qf.error && <span className="ml-2 text-error">{qf.error}</span>}
                    </p>
                  </div>
                </div>

                {/* Remove button (only for pending/error, not during upload or after success) */}
                {(qf.status === 'pending' || qf.status === 'error') && !isUploading && (
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => removeFile(qf.id)}
                    aria-label={`Remove ${qf.file.name}`}
                  >
                    <svg className="h-4 w-4" viewBox="0 0 20 20" fill="currentColor">
                      <path d="M6.28 5.22a.75.75 0 00-1.06 1.06L8.94 10l-3.72 3.72a.75.75 0 101.06 1.06L10 11.06l3.72 3.72a.75.75 0 101.06-1.06L11.06 10l3.72-3.72a.75.75 0 00-1.06-1.06L10 8.94 6.28 5.22z" />
                    </svg>
                  </Button>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Submit button */}
      {queuedFiles.length > 0 && (
        <div className="flex items-center justify-between">
          <p className="text-sm text-neutral-600">
            {isUploading
              ? 'Uploading...'
              : successCount > 0
                ? `${successCount} uploaded${pendingCount > 0 ? `, ${pendingCount} pending` : ''}${errorCount > 0 ? `, ${errorCount} failed` : ''}`
                : `${pendingCount} file(s) ready to upload`}
          </p>
          <Button
            onClick={handleSubmit}
            disabled={disabled || isUploading || pendingCount === 0}
          >
            {isUploading ? 'Uploading...' : `Upload ${pendingCount > 0 ? `(${pendingCount})` : ''}`}
          </Button>
        </div>
      )}
    </div>
  );
}
