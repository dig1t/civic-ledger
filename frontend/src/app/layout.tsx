import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import './globals.css';
import { Providers } from './providers';

const inter = Inter({
  subsets: ['latin'],
  display: 'swap',
  variable: '--font-inter',
});

export const metadata: Metadata = {
  title: 'CivicLedger - Secure Document Management',
  description:
    'FedRAMP-compliant secure document management system for government and defense organizations.',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className={inter.variable}>
      <body className="min-h-screen bg-neutral-50 font-sans text-neutral-900 antialiased">
        {/* Skip link for keyboard navigation - Section 508 requirement */}
        <a href="#main-content" className="skip-link">
          Skip to main content
        </a>

        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
