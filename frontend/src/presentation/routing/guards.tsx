import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from '../state/AuthContext';
import { SplashScreen } from './SplashScreen';

export interface RedirectState {
  from?: string;
}

/** Réservé aux utilisateurs connectés ; mémorise la page visée pour y revenir après connexion. */
export function ProtectedRoute() {
  const { status } = useAuth();
  const location = useLocation();
  if (status === 'checking') return <SplashScreen />;
  if (status === 'anonymous') {
    const state: RedirectState = { from: location.pathname + location.search };
    return <Navigate to="/login" replace state={state} />;
  }
  return <Outlet />;
}

/** Pages d'accueil publiques : un utilisateur déjà connecté part vers le tableau de bord. */
export function GuestRoute() {
  const { status } = useAuth();
  if (status === 'checking') return <SplashScreen />;
  if (status === 'authenticated') return <Navigate to="/" replace />;
  return <Outlet />;
}
