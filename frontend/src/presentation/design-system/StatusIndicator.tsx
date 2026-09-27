import { useI18n } from '../i18n/I18nContext';

export type Status = 'running' | 'stopped' | 'starting' | 'stopping';

export function StatusIndicator({ status }: { status: Status }) {
  const { t } = useI18n();
  return (
    <span className={`status status--${status}`}>
      <span className="status__dot" aria-hidden="true" />
      {t(`status.${status}`)}
    </span>
  );
}
