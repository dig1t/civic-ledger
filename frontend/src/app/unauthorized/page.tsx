import Link from 'next/link';

export default function UnauthorizedPage() {
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
        <div className="text-center">
          <h1 className="text-4xl font-bold text-neutral-900">Access Denied</h1>
          <p className="mt-4 text-lg text-neutral-600">
            You do not have permission to access this resource.
          </p>
          <p className="mt-2 text-neutral-500">
            Please contact your administrator if you believe this is an error.
          </p>

          <div className="mt-8 flex flex-wrap justify-center gap-4">
            <Link href="/dashboard" className="btn-primary">
              Go to Dashboard
            </Link>
            <Link href="/logout" className="btn-secondary">
              Sign Out
            </Link>
          </div>
        </div>
      </main>
    </div>
  );
}
