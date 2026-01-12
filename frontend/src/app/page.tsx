export default function HomePage() {
  return (
    <>
      <header className="border-b border-neutral-200 bg-white" role="banner">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4">
          <div className="flex items-center gap-2">
            <span
              className="text-xl font-bold text-primary-darker"
              aria-label="CivicLedger"
            >
              CivicLedger
            </span>
          </div>
          <nav aria-label="Main navigation">
            <ul className="flex items-center gap-6">
              <li>
                <a
                  href="/login"
                  className="text-neutral-700 hover:text-primary focus-visible:outline-none focus-visible:ring-focus focus-visible:ring-primary focus-visible:ring-offset-focus"
                >
                  Sign In
                </a>
              </li>
            </ul>
          </nav>
        </div>
      </header>

      <main id="main-content" className="mx-auto max-w-7xl px-4 py-12">
        <section aria-labelledby="hero-heading" className="text-center">
          <h1
            id="hero-heading"
            className="text-4xl font-bold text-neutral-900 sm:text-5xl"
          >
            Secure Document Management
          </h1>
          <p className="mx-auto mt-4 max-w-2xl text-lg text-neutral-600">
            FedRAMP and NIST 800-53 compliant document management system for
            government and defense organizations. Chain of custody tracking with
            immutable audit logs.
          </p>

          <div className="mt-8 flex flex-wrap justify-center gap-4">
            <a href="/login" className="btn-primary">
              Sign In
            </a>
            <a href="/about" className="btn-secondary">
              Learn More
            </a>
          </div>
        </section>

        <section
          aria-labelledby="features-heading"
          className="mt-16 grid gap-8 md:grid-cols-3"
        >
          <h2 id="features-heading" className="sr-only">
            Key Features
          </h2>

          <article className="card">
            <h3 className="text-xl font-semibold text-neutral-900">
              AES-256 Encryption
            </h3>
            <p className="mt-2 text-neutral-600">
              All documents encrypted at rest using AES-256-GCM. TLS 1.3 for
              data in transit.
            </p>
          </article>

          <article className="card">
            <h3 className="text-xl font-semibold text-neutral-900">
              Immutable Audit Logs
            </h3>
            <p className="mt-2 text-neutral-600">
              Complete chain of custody with write-once audit logs for every
              action. Non-repudiable records.
            </p>
          </article>

          <article className="card">
            <h3 className="text-xl font-semibold text-neutral-900">
              Role-Based Access
            </h3>
            <p className="mt-2 text-neutral-600">
              Strict RBAC with Administrator, Officer, and Auditor roles.
              Need-to-know access controls.
            </p>
          </article>
        </section>
      </main>

      <footer
        className="mt-auto border-t border-neutral-200 bg-white"
        role="contentinfo"
      >
        <div className="mx-auto max-w-7xl px-4 py-8">
          <p className="text-center text-sm text-neutral-500">
            CivicLedger - Secure Document Management System
          </p>
        </div>
      </footer>
    </>
  );
}
