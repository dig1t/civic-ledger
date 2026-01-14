import Link from 'next/link';

interface HeaderProps {
  user?: {
    name: string;
    role: 'ADMINISTRATOR' | 'OFFICER' | 'AUDITOR';
  } | null;
}

export function Header({ user }: HeaderProps) {
  return (
    <header className="border-b border-neutral-200 bg-white" role="banner">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4">
        <Link
          href={user ? '/dashboard' : '/'}
          className="text-xl font-bold text-primary-darker hover:text-primary focus-visible:outline-none focus-visible:ring-focus focus-visible:ring-primary focus-visible:ring-offset-focus"
        >
          CivicLedger
        </Link>

        <nav aria-label="Main navigation">
          <ul className="flex items-center gap-6">
            {user ? (
              <>
                <li>
                  <span className="text-sm text-neutral-600">
                    {user.name}{' '}
                    <span className="rounded bg-neutral-100 px-2 py-1 text-xs font-medium text-neutral-700">
                      {user.role}
                    </span>
                  </span>
                </li>
                <li>
                  <Link
                    href="/logout"
                    className="text-neutral-700 hover:text-primary focus-visible:outline-none focus-visible:ring-focus focus-visible:ring-primary focus-visible:ring-offset-focus"
                  >
                    Sign Out
                  </Link>
                </li>
              </>
            ) : (
              <li>
                <Link
                  href="/login"
                  className="text-neutral-700 hover:text-primary focus-visible:outline-none focus-visible:ring-focus focus-visible:ring-primary focus-visible:ring-offset-focus"
                >
                  Sign In
                </Link>
              </li>
            )}
          </ul>
        </nav>
      </div>
    </header>
  );
}
