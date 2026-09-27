import { useParams } from 'react-router-dom';
import { Alert, Button, Panel, Spinner } from '../design-system';
import { ScenarioCard } from '../features/scenario/ScenarioCard';
import { ScenarioSteps } from '../features/scenario/ScenarioSteps';
import { useI18n } from '../i18n/I18nContext';
import { useScenarios } from '../hooks/useScenarios';
import { NotFoundPage } from './NotFoundPage';

/**
 * Scénarios d'exercice, vue du joueur.
 * <p>
 * La même page sert la liste et le détail : le lien de l'URL décide. Le détail
 * vient de la liste déjà chargée — un scénario suivi n'a rien de plus à
 * demander, puisque son avancement est déjà là.
 */
export function ScenariosPage() {
  const { t } = useI18n();
  const { slug } = useParams();
  const { scenarios, loading, error, reload } = useScenarios();
  const opened = slug ? scenarios.find((scenario) => scenario.slug === slug) : null;

  if (slug && !loading && !opened) return <NotFoundPage />;

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <p className="page__eyebrow">{t('nav.scenarios')}</p>
          <h1 className="page__title">{opened ? opened.title : t('scenario.title')}</h1>
          <p className="page__lead">{opened ? opened.brief : t('scenario.lead')}</p>
        </div>
        <Button variant="ghost" size="sm" icon="refresh" loading={loading} onClick={() => void reload()}>
          {t('common.refresh')}
        </Button>
      </header>

      {error && (
        <Alert tone="error" title={t('scenario.loadError')}>
          {error.message}
        </Alert>
      )}

      {loading && scenarios.length === 0 ? (
        <div className="empty">
          <Spinner size={22} label={t('scenario.loading')} />
        </div>
      ) : opened ? (
        <Panel
          title={t('scenario.steps')}
          description={t('scenario.progress', { done: opened.progress.done, total: opened.progress.total })}
          actions={
            opened.progress.complete ? <span className="badge">{t('scenario.complete')}</span> : undefined
          }
        >
          <ScenarioSteps scenario={opened} />
        </Panel>
      ) : scenarios.length === 0 ? (
        <p className="empty">{t('scenario.empty')}</p>
      ) : (
        <div className="scenarios">
          {scenarios.map((scenario) => (
            <ScenarioCard key={scenario.slug} scenario={scenario} />
          ))}
        </div>
      )}
    </div>
  );
}
