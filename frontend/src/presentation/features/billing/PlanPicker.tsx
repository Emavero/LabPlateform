import { useState } from 'react';
import {
  formatMoney,
  methodsOf,
  PERIOD_ORDER,
  yearlySavings,
  type BillingPeriod,
  type PaymentMethod,
  type PlanOffer,
} from '@/domain/models/Billing';
import { Button, Icon } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';

interface PlanPickerProps {
  offers: readonly PlanOffer[];
  busy: boolean;
  onSubscribe: (method: PaymentMethod, period: BillingPeriod) => void;
}

/**
 * Choix de la durée puis du moyen de paiement.
 * <p>
 * La durée est demandée d'abord parce qu'elle change le prix de chaque moyen :
 * présenter l'inverse obligerait à réafficher tous les tarifs à chaque clic.
 * Un moyen annoncé indisponible par le serveur reste visible mais désactivé —
 * mieux vaut un bouton grisé qu'un parcours qui échoue au dernier écran.
 */
export function PlanPicker({ offers, busy, onSubscribe }: PlanPickerProps) {
  const { t, locale } = useI18n();
  const [period, setPeriod] = useState<BillingPeriod>('MONTHLY');
  const methods = methodsOf(offers);

  return (
    <div className="plan-picker">
      <div className="plan-picker__periods" role="group" aria-label={t('billing.periodGroup')}>
        {PERIOD_ORDER.map((candidate) => {
          const offer = offers.find((o) => o.period === candidate);
          if (!offer) return null;
          const savings = yearlySavings(offers, offer.method);
          return (
            <button
              key={candidate}
              type="button"
              className={['plan-period', period === candidate && 'plan-period--active'].filter(Boolean).join(' ')}
              aria-pressed={period === candidate}
              onClick={() => setPeriod(candidate)}
            >
              <span className="plan-period__name">{t(`billing.period.${candidate}`)}</span>
              {candidate === 'YEARLY' && savings > 0 && (
                <span className="plan-period__badge">−{savings} %</span>
              )}
            </button>
          );
        })}
      </div>

      <ul className="plan-picker__methods">
        {methods.map((method) => {
          const offer = offers.find((o) => o.method === method && o.period === period);
          if (!offer) return null;
          return (
            <li key={method} className={['plan-method', !offer.available && 'plan-method--off'].filter(Boolean).join(' ')}>
              <div className="plan-method__head">
                <Icon name={method === 'CARD' ? 'key' : 'wallet'} size={18} />
                <span className="plan-method__name">{offer.methodName}</span>
              </div>
              <p className="plan-method__price">{formatMoney(offer.price, locale)}</p>
              <p className="plan-method__hint">
                {offer.available ? t(`billing.hint.${method}`) : t('billing.methodOff')}
              </p>
              <Button
                size="sm"
                loading={busy}
                disabled={!offer.available}
                onClick={() => onSubscribe(method, period)}
              >
                {t('billing.pay', { price: formatMoney(offer.price, locale) })}
              </Button>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
