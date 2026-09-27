import { Link } from 'react-router-dom';
import type { AttackPath } from '@/domain/models/Exposure';
import { Icon } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';

/**
 * Chemin de progression, étape par étape.
 * <p>
 * L'effort porté par chaque étape est celui compté dans le total : la somme
 * affichée se refait à la main, ce qui rend le conseil vérifiable au lieu
 * d'être à croire.
 */
export function PathTrail({ path }: { path: AttackPath }) {
  const { t } = useI18n();

  return (
    <article className="trail">
      <header className="trail__head">
        <h3 className="trail__objective">
          <Link to={`/machines/${path.objectiveSlug}`}>{path.objectiveName}</Link>
        </h3>
        <p className="trail__meta">
          <span>{t('paths.effort', { effort: path.effort })}</span>
          <span>{t('paths.hops', { count: path.hops.length })}</span>
        </p>
      </header>

      <ol className="trail__hops">
        {path.hops.map((hop, index) => (
          <li key={`${hop.slug}-${index}`} className="trail__hop">
            <span className="trail__step" aria-hidden="true">
              {index + 1}
            </span>
            <span className="trail__body">
              <span className="trail__name">
                <Link to={`/machines/${hop.slug}`}>{hop.name}</Link>
                <span className="trail__service">
                  {hop.service.label} · {t('exposure.port', { port: hop.service.port })}
                </span>
              </span>
              <span className="trail__link">
                <Icon name={hop.link === 'ENTRY' ? 'target' : hop.link === 'SAME_SEGMENT' ? 'route' : 'copy'} size={13} />
                {hop.linkName}
              </span>
              <span className="trail__effort">{t('paths.effort', { effort: hop.effort })}</span>
            </span>
          </li>
        ))}
      </ol>
    </article>
  );
}
