'use client';

import { useRequireAuth } from '@/util/auth';
import { api } from '@/util/api';
import { UploadForm, type ClassificationLevel } from '@/features/documents';
import { useToast } from '@/components';

export default function UploadPage() {
  const { isAuthorized } = useRequireAuth('OFFICER', 'ADMINISTRATOR');
  const { showToast } = useToast();

  async function handleUpload(file: File, classification: ClassificationLevel) {
    await api.upload('/documents/upload', file, (progress) => {
      console.log(`Upload progress: ${progress}%`);
    }, { classificationLevel: classification });

    showToast(`${file.name} uploaded successfully`, 'success');
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

      <div className="card max-w-2xl">
        <UploadForm onUpload={handleUpload} />
      </div>
    </div>
  );
}
