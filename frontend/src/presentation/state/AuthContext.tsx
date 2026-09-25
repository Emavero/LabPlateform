import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import type { User } from '@/domain/models/User';
import type { Credentials, Registration } from '@/domain/repositories/AuthRepository';
import { useDependencies } from './DependenciesContext';

type SessionStatus = 'checking' | 'authenticated' | 'anonymous';

interface AuthState {
  readonly status: SessionStatus;
  readonly user: User | null;
  /** Vrai quand la session a expiré pendant l'utilisation (et non après une déconnexion volontaire). */
  readonly sessionExpired: boolean;
  login(credentials: Credentials): Promise<User>;
  register(registration: Registration): Promise<User>;
  logout(): Promise<void>;
}

const AuthContext = createContext<AuthState | null>(null);

/**
 * État de session global. La preuve de session est un cookie HttpOnly
 * illisible en JavaScript : l'état est donc reconstruit au chargement en
 * demandant au serveur qui est connecté.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const { auth, sessionMonitor } = useDependencies();
  const [user, setUser] = useState<User | null>(null);
  const [status, setStatus] = useState<SessionStatus>('checking');
  const [sessionExpired, setSessionExpired] = useState(false);
  const statusRef = useRef<SessionStatus>(status);
  statusRef.current = status;

  const openSession = useCallback((next: User) => {
    setUser(next);
    setStatus('authenticated');
    setSessionExpired(false);
    return next;
  }, []);

  useEffect(() => {
    let cancelled = false;
    auth.restoreSession
      .execute()
      .then((current) => {
        if (cancelled) return;
        if (current) openSession(current);
        else setStatus('anonymous');
      })
      .catch(() => {
        if (!cancelled) setStatus('anonymous');
      });
    return () => {
      cancelled = true;
    };
  }, [auth.restoreSession, openSession]);

  useEffect(
    () =>
      sessionMonitor.onSessionExpired(() => {
        // Un 401 n'est une « expiration » que si une session était ouverte :
        // la vérification initiale d'un visiteur anonyme renvoie aussi 401.
        if (statusRef.current !== 'authenticated') return;
        setUser(null);
        setStatus('anonymous');
        setSessionExpired(true);
      }),
    [sessionMonitor],
  );

  const login = useCallback(
    (credentials: Credentials) => auth.login.execute(credentials).then(openSession),
    [auth.login, openSession],
  );

  const register = useCallback(
    (registration: Registration) => auth.register.execute(registration).then(openSession),
    [auth.register, openSession],
  );

  const logout = useCallback(async () => {
    try {
      await auth.logout.execute();
    } finally {
      setUser(null);
      setStatus('anonymous');
      setSessionExpired(false);
    }
  }, [auth.logout]);

  const value = useMemo<AuthState>(
    () => ({ status, user, sessionExpired, login, register, logout }),
    [status, user, sessionExpired, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthState {
  const state = useContext(AuthContext);
  if (!state) throw new Error('useAuth doit être utilisé sous <AuthProvider>.');
  return state;
}
