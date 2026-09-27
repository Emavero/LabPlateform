import { Link } from 'react-router-dom';
import type { Box } from '@/domain/models/Box';
import { Icon, Panel } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';

/**
 * Ce que voit un compte gratuit à la place des informations d'attaque.
 * <p>
 * Le panneau dit ce qui manque et pourquoi, plutôt que d'afficher un refus :
 * la fiche de la machine reste consultable, c'est l'adresse, la cible à la
 * demande et les comptes rendus qui sont derrière l'abonnement.
 */
export function PaywallPanel({ box }: { box: Box }) {
  const { t } = useI18n();
  return (
    <Panel
      title={t('paywall.title')}
      description={t('paywall.subtitle', {
        name: box.name,
        difficulty: box.difficultyName,
        points: box.totalPoints,
      })}
    >
      <ul className="paywall__list">
        {(['paywall.address', 'paywall.instance', 'paywall.flags', 'paywall.writeups'] as const).map((key) => (
          <li key={key}>
            <Icon name="lock" size={16} /> {t(key)}
          </li>
        ))}
      </ul>
      <p className="paywall__note">{t('paywall.note')}</p>
      <Link className="btn btn--primary btn--sm" to="/abonnement">
        {t('paywall.cta')}
      </Link>
    </Panel>
  );
}
