import { countByLevel, firstDoor, EXPOSURE_LEVELS } from '@/domain/models/Exposure';
import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { StatTile } from '../features/analytics/StatTile';
import { ExposureCard } from '../features/exposure/ExposureCard';
import { useI18n } from '../i18n/I18nContext';
import { useExposure } from '../hooks/useExposure';

/**
 * Analyse d'exposition du lab.
 * <p>
 * La page dit d'où viennent ses notes, et le dit avant de les montrer : une
 * analyse qui ne s'explique pas se prend pour une vérité, et celle-ci n'est
 * qu'une lecture de ce que la plateforme déclare.
 */
export function ExposurePage() {
  const { t } = useI18n();
  const { exposure, loading, error, reload } = useExposure();
  const counts = exposure ? countByLevel(exposure.targets) : null;
  const door = exposure ? firstDoor(exposure.targets) : null;

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="page__eyebrow">{t('nav.exposure')}</p>
          <h1 className="page__title">{t('exposure.title')}</h1>
          <p className="page__lead">{t('exposure.lead')}</p>
        </div>
        <Button variant="ghost" size="sm" icon="refresh" loading={loading} onClick={() => void reload()}>
          {t('common.refresh')}
        </Button>
      </header>

      {error && (
        <Alert tone="error" title={t('exposure.loadError')}>
          {error.message}
        </Alert>
      )}

      {loading && !exposure ? (
        <div className="empty">
          <Spinner size={22} label={t('exposure.loading')} />
        </div>
      ) : (
        exposure &&
        counts && (
          <>
            <div className="kpi-row">
              {EXPOSURE_LEVELS.map((level) => (
                <StatTile key={level} label={t(`exposure.level.${level}`)} value={counts[level]} />
              ))}
            </div>

            {door && (
              <Alert tone="info" title={t('exposure.firstDoor', { name: door.name })}>
                {door.advice}
              </Alert>
            )}

            {!exposure.unlocked && exposure.lockedOut > 0 && (
              <Alert tone="info" title={t('exposure.lockedCount', { count: exposure.lockedOut })}>
                {t('billing.leadFree')}
              </Alert>
            )}

            <div className="page__grid">
              <Panel title={t('exposure.targets')}>
                {exposure.targets.length === 0 ? (
                  <p className="empty">{t('exposure.empty')}</p>
                ) : (
                  <div className="exposure-grid">
                    {exposure.targets.map((target) => (
                      <ExposureCard key={target.slug} target={target} />
                    ))}
                  </div>
                )}
              </Panel>

              <div className="support-detail">
                <Panel title={t('exposure.segments')}>
                  {exposure.segments.length === 0 ? (
                    <p className="empty">{t('common.empty')}</p>
                  ) : (
                    <ul className="tally">
                      {exposure.segments.map((segment) => (
                        <li key={segment.segment} className="tally__row">
                          <span className="tally__label">
                            <code>{segment.segment}.0/24</code>
                          </span>
                          <span className="tally__count">
                            {t('exposure.segmentTargets', { count: segment.targets })}
                          </span>
                        </li>
                      ))}
                    </ul>
                  )}
                </Panel>

                <Panel title={t('exposure.method')}>
                  <p className="method">
                    <Icon name="info" size={14} /> {t('exposure.methodText')}
                  </p>
                </Panel>
              </div>
            </div>
          </>
        )
      )}
    </div>
  );
}
