'use client';

import { type ReactNode } from 'react';
import { AuthProvider } from '@/util/auth';
import { ToastProvider } from '@/components';

export function Providers({ children }: { children: ReactNode }) {
  return (
    <ToastProvider>
      <AuthProvider>{children}</AuthProvider>
    </ToastProvider>
  );
}
