import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Alert, Button, Panel, Spinner } from '../design-system';
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
          <p className="page__eyebrow">Abonnement</p>
          <h1 className="page__title">Retour de paiement</h1>
        </div>
      </header>

      {!reference && (
        <Alert tone="error" title="Référence manquante">
          Cette page s’ouvre au retour d’un paiement. Reprenez depuis la page d’abonnement.
        </Alert>
      )}

      {error && (
        <Alert tone="error" title="Paiement non confirmé">
          {error.message}
        </Alert>
      )}

      {busy && <Spinner label="Vérification du paiement auprès du prestataire…" />}

      {done && (
        <Panel
          title={pro ? 'Abonnement actif' : 'Paiement non abouti'}
          description={
            pro
              ? 'Les machines du catalogue sont désormais accessibles.'
              : 'Le prestataire n’a pas confirmé l’encaissement. Rien n’a été débité de votre côté tant qu’il n’a pas abouti.'
          }
        >
          {pro && billing?.expiresAt && (
            <p className="billing__term">
              Accès jusqu’au {billing.expiresAt.toLocaleDateString('fr-FR')}
            </p>
          )}
          <div className="page__actions">
            <Link className="btn btn--primary btn--sm" to={pro ? '/machines' : '/abonnement'}>
              {pro ? 'Aller au catalogue' : 'Revenir aux formules'}
            </Link>
            {!pro && (
              <Button variant="ghost" size="sm" onClick={() => void confirm(reference).then(setDone)}>
                Vérifier à nouveau
              </Button>
            )}
          </div>
        </Panel>
      )}
    </div>
  );
}
