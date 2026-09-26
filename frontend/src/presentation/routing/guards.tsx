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

/**
 * Réservé aux administrateurs. Un utilisateur qui tape l'URL est renvoyé vers
 * son tableau de bord ; la vraie protection reste côté serveur, qui refuse
 * /api/admin/** à quiconque n'a pas le rôle.
 */
export function AdminRoute() {
  const { status, user } = useAuth();
  if (status === 'checking') return <SplashScreen />;
  if (status === 'anonymous') return <Navigate to="/login" replace />;
  if (user?.role !== 'ADMIN') return <Navigate to="/" replace />;
  return <Outlet />;
}

/**
 * Réservé aux pages de joueur. Un administrateur y est renvoyé vers son
 * tableau de bord : il publie le contenu, il ne le consomme pas, et lui
 * proposer le catalogue ou le classement n'aurait aucun sens — il n'a ni
 * abonnement, ni progression.
 * <p>
 * Les réglages du compte, eux, restent communs aux deux rôles.
 */
export function PlayerRoute() {
  const { status, user } = useAuth();
  if (status === 'checking') return <SplashScreen />;
  if (status === 'anonymous') return <Navigate to="/login" replace />;
  if (user?.role === 'ADMIN') return <Navigate to="/admin" replace />;
  return <Outlet />;
}

/** Pages d'accueil publiques : un utilisateur déjà connecté part vers le tableau de bord. */
export function GuestRoute() {
  const { status } = useAuth();
  if (status === 'checking') return <SplashScreen />;
  if (status === 'authenticated') return <Navigate to="/" replace />;
  return <Outlet />;
}
