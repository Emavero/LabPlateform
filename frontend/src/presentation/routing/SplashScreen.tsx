import { Logo, Spinner } from '../design-system';
import { useI18n } from '../i18n/I18nContext';

export function SplashScreen() {
  const { t } = useI18n();
  return (
    <div className="splash" role="status" aria-live="polite">
      <Logo />
      <Spinner size={22} />
      <span className="visually-hidden">{t('session.checking')}</span>
    </div>
  );
}
