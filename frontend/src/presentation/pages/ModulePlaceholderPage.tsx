import { useParams } from 'react-router-dom';
import { Icon, Panel } from '../design-system';
import { MODULES } from '../navigation/navigation';
import { NotFoundPage } from './NotFoundPage';

/** Page d'attente commune aux modules du menu qui ne sont pas encore livrés. */
export function ModulePlaceholderPage() {
  const { moduleId = '' } = useParams();
  const module = MODULES[moduleId];
  if (!module) return <NotFoundPage />;

  return (
    <div className="page">
      <header className="page__header">
        <div>
          <h1 className="page__title">{module.title}</h1>
          <p className="page__lead">{module.description}</p>
        </div>
      </header>
      <Panel className="placeholder">
        <Icon name="radar" size={40} className="placeholder__icon" />
        <p className="placeholder__title">Module en préparation</p>
        <p className="placeholder__text">
          Cette section arrive dans une prochaine version. Le lab Windows / Linux est déjà opérationnel.
        </p>
      </Panel>
    </div>
  );
}
