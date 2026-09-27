import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { PathTrail } from '../features/exposure/PathTrail';
import { useI18n } from '../i18n/I18nContext';
import { useExposure } from '../hooks/useExposure';

/**
 * Chemins d'attaque.
 * <p>
 * Les chemins viennent du même calcul que l'analyse d'exposition : la page les
 * lit du même appel, et annonce en clair qu'ils sont modélisés — un joueur qui
 * y lirait une reconnaissance réelle en tirerait de fausses conclusions.
 */
export function AttackPathsPage() {
  const { t } = useI18n();
  const { exposure, loading, error, reload } = useExposure();

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="page__eyebrow">{t('nav.attackPaths')}</p>
          <h1 className="page__title">{t('paths.title')}</h1>
          <p className="page__lead">{t('paths.lead')}</p>
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

      <Panel title={t('paths.method')}>
        <p className="method">
          <Icon name="info" size={14} /> {t('paths.methodText')}
        </p>
      </Panel>

      {loading && !exposure ? (
        <div className="empty">
          <Spinner size={22} label={t('paths.loading')} />
        </div>
      ) : (
        exposure && (
          <>
            {!exposure.unlocked && exposure.lockedOut > 0 && (
              <Alert tone="info" title={t('exposure.lockedCount', { count: exposure.lockedOut })}>
                {t('billing.leadFree')}
              </Alert>
            )}
            {exposure.paths.length === 0 ? (
              <p className="empty">{t('paths.empty')}</p>
            ) : (
              <div className="trails">
                {exposure.paths.map((path) => (
                  <PathTrail key={path.objectiveSlug} path={path} />
                ))}
              </div>
            )}
          </>
        )
      )}
    </div>
  );
}
