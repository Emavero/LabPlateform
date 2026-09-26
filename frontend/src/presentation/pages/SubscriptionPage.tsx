import { useMemo } from 'react';
import { daysLeft, formatMoney } from '@/domain/models/Billing';
import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { PlanPicker } from '../features/billing/PlanPicker';
import { useBilling } from '../hooks/useBilling';

const STATUS_LABELS: Record<string, string> = {
  ACTIVE: 'Actif',
  CANCELLED: 'Résilié, actif jusqu’à l’échéance',
  EXPIRED: 'Aucun abonnement',
};

const PAYMENT_STATUS_LABELS: Record<string, string> = {
  PENDING: 'En attente',
  SUCCEEDED: 'Encaissé',
  FAILED: 'Refusé',
  CANCELLED: 'Abandonné',
};

/**
 * Page d'abonnement : où en est le compte, ce qui lui est proposé, ce qu'il a
 * déjà payé.
 */
export function SubscriptionPage() {
  const { billing, loading, busy, error, subscribe, cancel, reload } = useBilling();
  const remaining = useMemo(() => (billing ? daysLeft(billing) : 0), [billing]);

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="page__eyebrow">Abonnement</p>
          <h1 className="page__title">{billing?.pro ? 'cyberMans Pro' : 'Passer à Pro'}</h1>
          <p className="page__lead">
            {billing?.pro
              ? `Accès complet au catalogue de machines. ${remaining} jour${remaining > 1 ? 's' : ''} restant${
                  remaining > 1 ? 's' : ''
                }.`
              : 'Les cours et le classement sont ouverts à tous. Les machines du catalogue et leurs cibles à la demande sont réservées aux abonnés.'}
          </p>
        </div>
        <Button variant="ghost" size="sm" icon="refresh" loading={loading} onClick={() => void reload()}>
          Actualiser
        </Button>
      </header>

      {error && (
        <Alert tone="error" title="Abonnement indisponible">
          {error.message}
        </Alert>
      )}

      {loading && !billing ? (
        <Spinner label="Chargement de votre abonnement…" />
      ) : (
        billing && (
          <div className="page__grid">
            <Panel
              title="Votre formule"
              description={STATUS_LABELS[billing.status] ?? billing.status}
              actions={
                billing.renewing ? (
                  <Button variant="ghost" size="sm" loading={busy} onClick={() => void cancel()}>
                    Résilier
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
                  {billing.renewing ? 'Prochain renouvellement le ' : 'Accès jusqu’au '}
                  {billing.expiresAt.toLocaleDateString('fr-FR')}
                </p>
              )}
              {billing.renewing && (
                <p className="billing__note">
                  La résiliation arrête le renouvellement sans rien retirer : la période déjà payée reste à vous.
                </p>
              )}
            </Panel>

            {!billing.pro && (
              <Panel title="Choisir une formule" description="Paiement par carte ou par portefeuille mobile.">
                <PlanPicker offers={billing.offers} busy={busy} onSubscribe={(m, p) => void subscribe(m, p)} />
              </Panel>
            )}

            <Panel title="Paiements" description="Les dix derniers.">
              {billing.payments.length === 0 ? (
                <p className="empty">Aucun paiement pour l’instant.</p>
              ) : (
                <ul className="billing__payments">
                  {billing.payments.map((payment) => (
                    <li key={payment.reference} className="billing__payment">
                      <span className="billing__payment-date">
                        {payment.createdAt.toLocaleDateString('fr-FR')}
                      </span>
                      <span className="billing__payment-amount">{formatMoney(payment.amount)}</span>
                      <span className="billing__payment-method">{payment.methodName}</span>
                      <span className={`billing__payment-status billing__payment-status--${payment.status.toLowerCase()}`}>
                        {PAYMENT_STATUS_LABELS[payment.status] ?? payment.status}
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
