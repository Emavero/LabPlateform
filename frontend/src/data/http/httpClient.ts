import axios, { type AxiosError, type AxiosInstance } from 'axios';
import { AppError, type AppErrorKind } from '@/domain/errors/AppError';
import type { SessionMonitor } from '@/domain/repositories/SessionMonitor';

interface ApiErrorBody {
  message?: string;
  details?: string[];
}

const KIND_BY_STATUS: Record<number, AppErrorKind> = {
  400: 'validation',
  401: 'unauthorized',
  404: 'not_found',
  409: 'conflict',
};

/**
 * Client HTTP unique. La session voyage dans un cookie HttpOnly posé par le
 * backend : le code JavaScript ne manipule jamais de jeton, il demande
 * simplement au navigateur d'envoyer les cookies (withCredentials).
 */
export function createHttpClient(): { http: AxiosInstance; sessionMonitor: SessionMonitor } {
  const listeners = new Set<() => void>();

  const http = axios.create({
    baseURL: '/api',
    withCredentials: true,
    // Un démarrage de machine réelle (conteneur) peut prendre plusieurs secondes.
    timeout: 60_000,
    headers: { 'Content-Type': 'application/json' },
  });

  http.interceptors.response.use(
    (response) => response,
    (error: AxiosError<ApiErrorBody>) => {
      const appError = toAppError(error);
      const isAuthEndpoint = error.config?.url?.startsWith('/auth/') ?? false;
      if (appError.kind === 'unauthorized' && !isAuthEndpoint) {
        listeners.forEach((listener) => listener());
      }
      return Promise.reject(appError);
    },
  );

  const sessionMonitor: SessionMonitor = {
    onSessionExpired(listener) {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
  };

  return { http, sessionMonitor };
}

function toAppError(error: AxiosError<ApiErrorBody>): AppError {
  if (!error.response) {
    return new AppError('network', 'Le serveur est injoignable. Vérifiez votre connexion puis réessayez.');
  }
  const { status } = error.response;
  const data = parseBody(error.response.data);
  const kind = KIND_BY_STATUS[status] ?? 'unexpected';
  const message =
    data?.message ??
    (kind === 'unexpected' ? 'Le serveur a rencontré une erreur. Réessayez dans un instant.' : 'Requête refusée.');
  return new AppError(kind, message);
}

/** Les téléchargements demandent du texte brut : une erreur JSON arrive alors sous forme de chaîne. */
function parseBody(data: unknown): ApiErrorBody | undefined {
  if (typeof data === 'string') {
    try {
      return JSON.parse(data) as ApiErrorBody;
    } catch {
      return undefined;
    }
  }
  return data as ApiErrorBody | undefined;
}
