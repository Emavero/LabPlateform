import type { ConnectionInfo } from '@/domain/models/VirtualMachine';
import { CopyField, Icon } from '../../design-system';
import { ACCESS_GUIDES } from './accessGuides';

/** Panneau d'accès affiché dès qu'une machine tourne : identifiants + mode d'emploi. */
export function ConnectionDetails({ connection }: { connection: ConnectionInfo }) {
  const guide = ACCESS_GUIDES[connection.protocol];
  const command = guide.command(connection);

  return (
    <div className="access">
      <div className="access__grid">
        <CopyField label="Adresse IP" value={connection.host} />
        <CopyField label={`Port ${connection.protocol}`} value={String(connection.port)} />
        <CopyField label="Utilisateur" value={connection.username} />
        <CopyField label="Mot de passe temporaire" value={connection.password} secret />
      </div>

      <div className="terminal" aria-label={`Commande de connexion ${guide.client}`}>
        <div className="terminal__bar">
          <span className="terminal__dots" aria-hidden="true">
            <i />
            <i />
            <i />
          </span>
          <span className="terminal__title">
            <Icon name="terminal" size={14} /> {guide.client}
          </span>
        </div>
        <div className="terminal__body">
          <CopyField label="Commande" value={command} />
        </div>
      </div>

      <ol className="access__steps">
        {guide.steps(connection).map((step) => (
          <li key={step}>{step}</li>
        ))}
      </ol>
    </div>
  );
}
