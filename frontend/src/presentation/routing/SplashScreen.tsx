import { Logo, Spinner } from '../design-system';

export function SplashScreen() {
  return (
    <div className="splash" role="status" aria-live="polite">
      <Logo />
      <Spinner size={22} />
      <span className="visually-hidden">Vérification de la session…</span>
    </div>
  );
}
