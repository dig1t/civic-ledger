'use client';

import { useState, type FormEvent } from 'react';
import Link from 'next/link';
import { useAuth } from '@/lib/auth';
import { Button } from '@/components/ui/button';
import { FormField } from '@/components/ui/form-field';

export default function LoginPage() {
  const { login } = useAuth();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [formData, setFormData] = useState({
    email: '',
    password: '',
    mfaCode: '',
  });

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setIsLoading(true);
    setError(null);

    try {
      await login(formData.email, formData.password, formData.mfaCode);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen flex-col">
      <header className="border-b border-neutral-200 bg-white" role="banner">
        <div className="mx-auto flex max-w-7xl items-center px-4 py-4">
          <Link
            href="/"
            className="text-xl font-bold text-primary-darker hover:text-primary focus-visible:outline-none focus-visible:ring-focus focus-visible:ring-primary focus-visible:ring-offset-focus"
          >
            CivicLedger
          </Link>
        </div>
      </header>

      <main
        id="main-content"
        className="flex flex-1 items-center justify-center px-4 py-12"
      >
        <div className="w-full max-w-md">
          <div className="card">
            <h1 className="text-2xl font-bold text-neutral-900">Sign In</h1>
            <p className="mt-2 text-neutral-600">
              Access the secure document management system.
            </p>

            {error && (
              <div
                className="alert-error mt-4"
                role="alert"
                aria-live="assertive"
              >
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} className="mt-6 space-y-4">
              <FormField
                label="Email Address"
                type="email"
                name="email"
                autoComplete="email"
                required
                value={formData.email}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, email: e.target.value }))
                }
                disabled={isLoading}
              />

              <FormField
                label="Password"
                type="password"
                name="password"
                autoComplete="current-password"
                required
                value={formData.password}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, password: e.target.value }))
                }
                disabled={isLoading}
              />

              <FormField
                label="MFA Code"
                type="text"
                name="mfaCode"
                inputMode="numeric"
                pattern="[0-9]*"
                autoComplete="one-time-code"
                hint="Optional in dev mode. Use 123456 if MFA is enabled."
                value={formData.mfaCode}
                onChange={(e) =>
                  setFormData((prev) => ({ ...prev, mfaCode: e.target.value }))
                }
                disabled={isLoading}
              />

              <Button
                type="submit"
                className="w-full"
                isLoading={isLoading}
              >
                Sign In
              </Button>
            </form>
          </div>

          <p className="mt-4 text-center text-sm text-neutral-500">
            This is a U.S. Government system. Unauthorized access is prohibited.
          </p>
        </div>
      </main>
    </div>
  );
}
