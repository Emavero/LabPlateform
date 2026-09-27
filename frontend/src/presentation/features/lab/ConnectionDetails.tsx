import type { ConnectionInfo } from '@/domain/models/VirtualMachine';
import { CopyField, Icon } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';
import { ACCESS_GUIDES } from './accessGuides';

/** Panneau d'accès affiché dès qu'une machine tourne : identifiants + mode d'emploi. */
export function ConnectionDetails({ connection }: { connection: ConnectionInfo }) {
  const { t } = useI18n();
  const guide = ACCESS_GUIDES[connection.protocol];
  const command = guide.command(connection);
  const client = t(guide.client);
  const steps = { user: connection.username, host: connection.host, port: connection.port };

  return (
    <div className="access">
      <div className="access__grid">
        <CopyField label={t('access.ip')} value={connection.host} />
        <CopyField label={t('access.port', { protocol: connection.protocol })} value={String(connection.port)} />
        <CopyField label={t('access.user')} value={connection.username} />
        <CopyField label={t('access.password')} value={connection.password} secret />
      </div>

      <div className="terminal" aria-label={t('access.commandFor', { client })}>
        <div className="terminal__bar">
          <span className="terminal__dots" aria-hidden="true">
            <i />
            <i />
            <i />
          </span>
          <span className="terminal__title">
            <Icon name="terminal" size={14} /> {client}
          </span>
        </div>
        <div className="terminal__body">
          <CopyField label={t('access.command')} value={command} />
        </div>
      </div>

      <ol className="access__steps">
        {guide.steps.map((step) => (
          <li key={step}>{t(step, steps)}</li>
        ))}
      </ol>
    </div>
  );
}
