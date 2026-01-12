'use client';

import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react';
import { useRouter } from 'next/navigation';
import { api, ApiError } from './api';

export type UserRole = 'ADMINISTRATOR' | 'OFFICER' | 'AUDITOR';

export interface User {
  id: string;
  email: string;
  name: string;
  role: UserRole;
}

interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  login: (email: string, password: string, mfaCode: string) => Promise<void>;
  logout: () => void;
  hasRole: (...roles: UserRole[]) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      api
        .get<User>('/auth/me')
        .then(setUser)
        .catch(() => {
          localStorage.removeItem('token');
        })
        .finally(() => setIsLoading(false));
    } else {
      setIsLoading(false);
    }
  }, []);

  async function login(email: string, password: string, mfaCode: string) {
    const response = await fetch(
      `${process.env.NEXT_PUBLIC_API_URL}/auth/login`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-MFA-Code': mfaCode,
        },
        body: JSON.stringify({ email, password }),
      }
    );

    if (!response.ok) {
      const data = await response.json().catch(() => ({}));
      throw new ApiError(
        data.message || 'Authentication failed',
        response.status
      );
    }

    const data = await response.json();
    localStorage.setItem('token', data.token);
    setUser(data.user);
    router.push('/dashboard');
  }

  function logout() {
    localStorage.removeItem('token');
    setUser(null);
    router.push('/login');
  }

  function hasRole(...roles: UserRole[]) {
    return user ? roles.includes(user.role) : false;
  }

  return (
    <AuthContext.Provider value={{ user, isLoading, login, logout, hasRole }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

export function useRequireAuth(...allowedRoles: UserRole[]) {
  const { user, isLoading, hasRole } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoading) {
      if (!user) {
        router.push('/login');
      } else if (allowedRoles.length > 0 && !hasRole(...allowedRoles)) {
        router.push('/unauthorized');
      }
    }
  }, [user, isLoading, hasRole, router, allowedRoles]);

  return { user, isLoading, isAuthorized: user && (allowedRoles.length === 0 || hasRole(...allowedRoles)) };
}
