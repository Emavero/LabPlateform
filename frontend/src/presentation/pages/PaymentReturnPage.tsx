import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Alert, Button, Panel, Spinner } from '../design-system';
import { useI18n } from '../i18n/I18nContext';
import { useBilling } from '../hooks/useBilling';

/**
 * Page de retour du prestataire de paiement.
 * <p>
 * Elle ne conclut rien elle-même : elle signale au serveur que le payeur est
 * revenu, et c'est le serveur qui interroge le prestataire. Un navigateur qui
 * appellerait cette page à la main n'obtiendrait donc pas d'abonnement.
 * <p>
 * La notification du prestataire peut arriver avant ce retour, ou après : dans
 * les deux cas le résultat est le même, le paiement n'étant crédité qu'une fois.
 */
export function PaymentReturnPage() {
  const { t, formatDate } = useI18n();
  const [params] = useSearchParams();
  const reference = params.get('reference') ?? '';
  const { billing, busy, error, confirm } = useBilling();
  const [done, setDone] = useState(false);
  const asked = useRef(false);

  useEffect(() => {
    if (asked.current || !reference) return;
    asked.current = true;
    void confirm(reference).then(setDone);
  }, [confirm, reference]);

  const pro = billing?.pro ?? false;

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="page__eyebrow">{t('billing.eyebrow')}</p>
          <h1 className="page__title">{t('billing.return.title')}</h1>
        </div>
      </header>

      {!reference && (
        <Alert tone="error" title={t('billing.return.missing')}>
          {t('billing.return.missingText')}
        </Alert>
      )}

      {error && (
        <Alert tone="error" title={t('billing.return.failed')}>
          {error.message}
        </Alert>
      )}

      {busy && <Spinner label={t('billing.return.checking')} />}

      {done && (
        <Panel
          title={t(pro ? 'billing.return.ok' : 'billing.return.ko')}
          description={t(pro ? 'billing.return.okText' : 'billing.return.koText')}
        >
          {pro && billing?.expiresAt && (
            <p className="billing__term">
              {t('billing.accessUntil', { date: formatDate(billing.expiresAt) })}
            </p>
          )}
          <div className="page__actions">
            <Link className="btn btn--primary btn--sm" to={pro ? '/machines' : '/abonnement'}>
              {t(pro ? 'billing.return.toCatalogue' : 'billing.return.toPlans')}
            </Link>
            {!pro && (
              <Button variant="ghost" size="sm" onClick={() => void confirm(reference).then(setDone)}>
                {t('billing.return.recheck')}
              </Button>
            )}
          </div>
        </Panel>
      )}
    </div>
  );
}
