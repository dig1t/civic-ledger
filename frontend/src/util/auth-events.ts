// Event emitter for auth-related events that can be triggered from non-React code (like API client)

type AuthEventType = 'auth:failure' | 'auth:logout';

interface AuthEvent {
  type: AuthEventType;
  message?: string;
}

type AuthEventListener = (event: AuthEvent) => void;

class AuthEventEmitter {
  private listeners: Set<AuthEventListener> = new Set();

  subscribe(listener: AuthEventListener): () => void {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  }

  emit(event: AuthEvent): void {
    this.listeners.forEach((listener) => {
      try {
        listener(event);
      } catch (error) {
        console.error('Auth event listener error:', error);
      }
    });
  }
}

export const authEvents = new AuthEventEmitter();

// Convenience function for API to call
export function emitAuthFailure(message?: string): void {
  authEvents.emit({ type: 'auth:failure', message: message || 'Your session has expired. Please log in again.' });
}
