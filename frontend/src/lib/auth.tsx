'use client';

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react';
import { useRouter } from 'next/navigation';
import { api, ApiError } from './api';
import { authEvents } from './auth-events';
import { useToast } from '@/components/ui/toast';

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
  const { showToast } = useToast();
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  // Handle auth failure events from API
  const handleAuthFailure = useCallback((message: string) => {
    localStorage.removeItem('token');
    setUser(null);
    showToast(message, 'error');
    router.push('/login');
  }, [router, showToast]);

  // Subscribe to auth events
  useEffect(() => {
    const unsubscribe = authEvents.subscribe((event) => {
      if (event.type === 'auth:failure') {
        handleAuthFailure(event.message || 'Authentication failed');
      }
    });

    return unsubscribe;
  }, [handleAuthFailure]);

  // Load user on mount
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      api
        .get<{ id: string; email: string; fullName: string; roles: string[] }>('/auth/me')
        .then((data) => {
          // Map backend user DTO to frontend User interface
          setUser({
            id: data.id,
            email: data.email,
            name: data.fullName,
            role: data.roles[0] as UserRole,
          });
        })
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
    localStorage.setItem('token', data.accessToken);

    // Map backend user DTO to frontend User interface
    const user: User = {
      id: data.user.id,
      email: data.user.email,
      name: data.user.fullName,
      role: data.user.roles[0] as UserRole, // Primary role
    };
    setUser(user);
    router.push('/dashboard');
  }

  function logout() {
    localStorage.removeItem('token');
    setUser(null);
    showToast('You have been logged out', 'info');
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
