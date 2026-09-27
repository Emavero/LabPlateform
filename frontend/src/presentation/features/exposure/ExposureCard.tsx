import { Link } from 'react-router-dom';
import { scoreWidth, type TargetExposure } from '@/domain/models/Exposure';
import { Icon } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';

/**
 * Exposition d'une cible.
 * <p>
 * Le niveau se lit trois fois : par le mot, par l'icône, et par la longueur de
 * la barre. La couleur ne fait que confirmer — une page d'analyse doit rester
 * lisible pour qui ne distingue pas le rouge du vert.
 */
export function ExposureCard({ target }: { target: TargetExposure }) {
  const { t } = useI18n();

  return (
    <article className={`exposure exposure--${target.level.toLowerCase()}`}>
      <header className="exposure__head">
        <h3 className="exposure__name">
          {target.locked ? target.name : <Link to={`/machines/${target.slug}`}>{target.name}</Link>}
        </h3>
        <p className="exposure__level">
          <Icon name={target.level === 'CRITICAL' || target.level === 'HIGH' ? 'alert' : 'shield'} size={14} />
          {t(`exposure.level.${target.level}`)}
        </p>
      </header>

      <div
        className="exposure__gauge"
        role="img"
        aria-label={t('exposure.score', { score: target.score })}
        title={t('exposure.score', { score: target.score })}
      >
        <span className="exposure__bar" style={{ width: `${scoreWidth(target.score)}%` }} />
      </div>

      <dl className="exposure__facts">
        <div>
          <dt>{t('exposure.service')}</dt>
          <dd>
            {target.service.label} · {t('exposure.port', { port: target.service.port })}
          </dd>
        </div>
        <div>
          <dt>{t('exposure.address')}</dt>
          <dd>
            {target.locked ? (
              <span className="badge badge--locked">
                <Icon name="lock" size={12} /> {t('exposure.locked')}
              </span>
            ) : (
              <code>{target.address}</code>
            )}
          </dd>
        </div>
      </dl>

      <p className="exposure__subtitle">{t('exposure.why')}</p>
      <ul className="exposure__signals">
        {target.signals.map((signal) => (
          <li key={signal.code}>{signal.label}</li>
        ))}
      </ul>

      <p className="exposure__advice">
        <Icon name="info" size={14} /> {target.advice}
      </p>
    </article>
  );
}
