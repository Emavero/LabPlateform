import { formatDuration, type LearningProgress } from '@/domain/models/Course';
import { useI18n } from '../../i18n/I18nContext';

/** Avancement d'une filière, sous forme de barre et de compteurs. */
export function TrackProgress({ progress }: { progress: LearningProgress }) {
  const { t } = useI18n();
  const percent = Math.round(progress.ratio * 100);

  return (
    <div className="track-progress">
      <div className="progress__bar" role="img" aria-label={t('track.percentDone', { percent })}>
        <span className="progress__bar-fill" style={{ width: `${percent}%` }} />
      </div>
      <dl className="progress__stats track-progress__stats">
        <div>
          <dt>{t('track.coursesDone')}</dt>
          <dd>
            {progress.coursesCompleted}
            <span className="progress__total"> / {progress.courses}</span>
          </dd>
        </div>
        <div>
          <dt>{t('track.sectionsLabel')}</dt>
          <dd>
            {progress.sectionsCompleted}
            <span className="progress__total"> / {progress.sections}</span>
          </dd>
        </div>
        <div>
          <dt>{t('track.timeSpent')}</dt>
          <dd>{formatDuration(progress.minutesDone)}</dd>
        </div>
      </dl>
    </div>
  );
}
