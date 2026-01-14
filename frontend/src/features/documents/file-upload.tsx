'use client';

import { useCallback, useId, useRef, useState, type DragEvent } from 'react';
import { cn } from '@/util/utils';
import { Button } from '@/components';

export type UploadStatus = 'idle' | 'uploading' | 'success' | 'error';

interface FileUploadProps {
  onUpload: (file: File) => Promise<void>;
  accept?: string;
  maxSize?: number;
  disabled?: boolean;
}

export function FileUpload({
  onUpload,
  accept = '.pdf,.doc,.docx,.txt',
  maxSize = 50 * 1024 * 1024, // 50MB default
  disabled = false,
}: FileUploadProps) {
  const inputId = useId();
  const inputRef = useRef<HTMLInputElement>(null);
  const [status, setStatus] = useState<UploadStatus>('idle');
  const [progress, setProgress] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [fileName, setFileName] = useState<string | null>(null);

  const handleFile = useCallback(
    async (file: File) => {
      // Validate file size
      if (file.size > maxSize) {
        setError(`File size exceeds maximum allowed (${Math.round(maxSize / 1024 / 1024)}MB)`);
        setStatus('error');
        return;
      }

      // Validate file extension
      const allowedExtensions = accept.split(',').map((ext) => ext.trim().toLowerCase());
      const fileExtension = '.' + file.name.split('.').pop()?.toLowerCase();
      if (!allowedExtensions.includes(fileExtension)) {
        setError(`File type not allowed. Accepted types: ${accept}`);
        setStatus('error');
        return;
      }

      setFileName(file.name);
      setStatus('uploading');
      setProgress(0);
      setError(null);

      try {
        await onUpload(file);
        setProgress(100);
        setStatus('success');
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Upload failed');
        setStatus('error');
      }
    },
    [onUpload, maxSize, accept]
  );

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      handleFile(file);
    }
  };

  const handleDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(false);

    if (disabled || status === 'uploading') return;

    const file = e.dataTransfer.files[0];
    if (file) {
      handleFile(file);
    }
  };

  const handleDragOver = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    if (!disabled && status !== 'uploading') {
      setIsDragging(true);
    }
  };

  const handleDragLeave = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const resetUpload = () => {
    setStatus('idle');
    setProgress(0);
    setError(null);
    setFileName(null);
    if (inputRef.current) {
      inputRef.current.value = '';
    }
  };

  return (
    <div className="space-y-4">
      {/* Visually hidden live region for screen readers */}
      <div
        aria-live="polite"
        aria-atomic="true"
        className="sr-only"
        role="status"
      >
        {status === 'uploading' && `Uploading ${fileName}, ${progress}% complete`}
        {status === 'success' && `${fileName} uploaded successfully`}
        {status === 'error' && `Upload failed: ${error}`}
      </div>

      {/* eslint-disable-next-line jsx-a11y/no-noninteractive-element-interactions */}
      <div
        className={cn(
          'relative rounded-lg border-2 border-dashed p-8 text-center transition-colors',
          isDragging
            ? 'border-primary bg-primary-lighter'
            : status === 'error'
              ? 'border-error bg-error-lighter'
              : status === 'success'
                ? 'border-success bg-success-lighter'
                : 'border-neutral-300 hover:border-neutral-400',
          (disabled || status === 'uploading') && 'cursor-not-allowed opacity-50'
        )}
        onDrop={handleDrop}
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        role="region"
        aria-labelledby={`${inputId}-label`}
      >
        <input
          ref={inputRef}
          id={inputId}
          type="file"
          accept={accept}
          onChange={handleInputChange}
          disabled={disabled || status === 'uploading'}
          className="absolute inset-0 cursor-pointer opacity-0 disabled:cursor-not-allowed"
          aria-describedby={`${inputId}-description ${inputId}-status`}
        />

        <div className="space-y-2">
          <div className="mx-auto h-12 w-12 text-neutral-400" aria-hidden="true">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
              strokeWidth={1.5}
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                d="M3 16.5v2.25A2.25 2.25 0 005.25 21h13.5A2.25 2.25 0 0021 18.75V16.5m-13.5-9L12 3m0 0l4.5 4.5M12 3v13.5"
              />
            </svg>
          </div>

          <p id={`${inputId}-label`} className="text-lg font-medium text-neutral-900">
            {status === 'uploading'
              ? 'Uploading...'
              : status === 'success'
                ? 'Upload complete'
                : 'Drop file here or click to upload'}
          </p>

          <p id={`${inputId}-description`} className="text-sm text-neutral-500">
            Accepted formats: {accept} (max {Math.round(maxSize / 1024 / 1024)}MB)
          </p>
        </div>
      </div>

      {/* Progress bar */}
      {status === 'uploading' && (
        <div className="space-y-2">
          <div className="flex items-center justify-between text-sm">
            <span className="truncate font-medium text-neutral-700">{fileName}</span>
            <span className="text-neutral-500">{progress}%</span>
          </div>
          <div
            className="h-2 overflow-hidden rounded-full bg-neutral-200"
            role="progressbar"
            aria-valuenow={progress}
            aria-valuemin={0}
            aria-valuemax={100}
            aria-label={`Upload progress: ${progress}%`}
          >
            <div
              className="h-full bg-primary transition-all duration-300"
              style={{ width: `${progress}%` }}
            />
          </div>
        </div>
      )}

      {/* Success message */}
      {status === 'success' && (
        <div className="flex items-center justify-between rounded-lg bg-success-lighter p-4">
          <div className="flex items-center gap-2">
            <svg
              className="h-5 w-5 text-success"
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 20 20"
              fill="currentColor"
              aria-hidden="true"
            >
              <path
                fillRule="evenodd"
                d="M16.704 4.153a.75.75 0 01.143 1.052l-8 10.5a.75.75 0 01-1.127.075l-4.5-4.5a.75.75 0 011.06-1.06l3.894 3.893 7.48-9.817a.75.75 0 011.05-.143z"
                clipRule="evenodd"
              />
            </svg>
            <span className="font-medium text-success-dark">{fileName}</span>
          </div>
          <Button variant="ghost" size="sm" onClick={resetUpload}>
            Upload another
          </Button>
        </div>
      )}

      {/* Error message */}
      {status === 'error' && (
        <div
          className="flex items-center justify-between rounded-lg bg-error-lighter p-4"
          role="alert"
        >
          <div className="flex items-center gap-2">
            <svg
              className="h-5 w-5 text-error"
              xmlns="http://www.w3.org/2000/svg"
              viewBox="0 0 20 20"
              fill="currentColor"
              aria-hidden="true"
            >
              <path
                fillRule="evenodd"
                d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-8-5a.75.75 0 01.75.75v4.5a.75.75 0 01-1.5 0v-4.5A.75.75 0 0110 5zm0 10a1 1 0 100-2 1 1 0 000 2z"
                clipRule="evenodd"
              />
            </svg>
            <span className="text-error-dark">{error}</span>
          </div>
          <Button variant="ghost" size="sm" onClick={resetUpload}>
            Try again
          </Button>
        </div>
      )}

      <p id={`${inputId}-status`} className="sr-only">
        {status === 'idle' && 'Ready to upload'}
        {status === 'uploading' && `Uploading ${progress}%`}
        {status === 'success' && 'Upload successful'}
        {status === 'error' && `Error: ${error}`}
      </p>
    </div>
  );
}
