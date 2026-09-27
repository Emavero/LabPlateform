import { Link } from 'react-router-dom';
import { useI18n } from '../i18n/I18nContext';

export function NotFoundPage() {
  const { t } = useI18n();
  return (
    <div className="page page--center">
      <p className="page__eyebrow">Erreur 404</p>
      <h1 className="page__title">{t('notFound.title')}</h1>
      <p className="page__lead">{t('notFound.text')}</p>
      <Link className="btn btn--primary btn--md" to="/">
        {t('notFound.back')}
      </Link>
    </div>
  );
}
