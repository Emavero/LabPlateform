import { useMemo } from 'react';
import { daysLeft, formatMoney } from '@/domain/models/Billing';
import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { PlanPicker } from '../features/billing/PlanPicker';
import { useI18n } from '../i18n/I18nContext';
import { useBilling } from '../hooks/useBilling';

/**
 * Page d'abonnement : où en est le compte, ce qui lui est proposé, ce qu'il a
 * déjà payé.
 */
export function SubscriptionPage() {
  const { t, locale, formatDate } = useI18n();
  const { billing, loading, busy, error, subscribe, cancel, reload } = useBilling();
  const remaining = useMemo(() => (billing ? daysLeft(billing) : 0), [billing]);

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="page__eyebrow">{t('billing.eyebrow')}</p>
          <h1 className="page__title">{t(billing?.pro ? 'billing.titlePro' : 'billing.titleFree')}</h1>
          <p className="page__lead">
            {billing?.pro ? t('billing.leadPro', { days: remaining }) : t('billing.leadFree')}
          </p>
        </div>
        <Button variant="ghost" size="sm" icon="refresh" loading={loading} onClick={() => void reload()}>
          {t('common.refresh')}
        </Button>
      </header>

      {error && (
        <Alert tone="error" title={t('billing.unavailable')}>
          {error.message}
        </Alert>
      )}

      {loading && !billing ? (
        <Spinner label={t('common.loading')} />
      ) : (
        billing && (
          <div className="page__grid">
            <Panel
              title={t('billing.yourPlan')}
              description={t(`billing.status.${billing.status}`)}
              actions={
                billing.renewing ? (
                  <Button variant="ghost" size="sm" loading={busy} onClick={() => void cancel()}>
                    {t('billing.cancel')}
                  </Button>
                ) : undefined
              }
            >
              <p className="billing__plan">
                <Icon name={billing.pro ? 'crown' : 'user'} size={20} />
                <span>{billing.planName}</span>
              </p>
              {billing.expiresAt && (
                <p className="billing__term">
                  {t(billing.renewing ? 'billing.renewalOn' : 'billing.accessUntil', {
                    date: formatDate(billing.expiresAt),
                  })}
                </p>
              )}
              {billing.renewing && (
                <p className="billing__note">{t('billing.cancelNote')}</p>
              )}
            </Panel>

            {!billing.pro && (
              <Panel title={t('billing.choosePlan')} description={t('billing.chooseHint')}>
                <PlanPicker offers={billing.offers} busy={busy} onSubscribe={(m, p) => void subscribe(m, p)} />
              </Panel>
            )}

            <Panel title={t('billing.payments')} description={t('billing.paymentsHint')}>
              {billing.payments.length === 0 ? (
                <p className="empty">{t('billing.noPayment')}</p>
              ) : (
                <ul className="billing__payments">
                  {billing.payments.map((payment) => (
                    <li key={payment.reference} className="billing__payment">
                      <span className="billing__payment-date">{formatDate(payment.createdAt)}</span>
                      <span className="billing__payment-amount">{formatMoney(payment.amount, locale)}</span>
                      <span className="billing__payment-method">{payment.methodName}</span>
                      <span className={`billing__payment-status billing__payment-status--${payment.status.toLowerCase()}`}>
                        {t(`billing.payment.${payment.status}`)}
                      </span>
                    </li>
                  ))}
                </ul>
              )}
            </Panel>
          </div>
        )
      )}
    </div>
  );
}
