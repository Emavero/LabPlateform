import { formatDuration, type LearningProgress } from '@/domain/models/Course';

/** Avancement d'une filière, sous forme de barre et de compteurs. */
export function TrackProgress({ progress }: { progress: LearningProgress }) {
  const percent = Math.round(progress.ratio * 100);

  return (
    <div className="track-progress">
      <div className="progress__bar" role="img" aria-label={`${percent} % de la filière terminée`}>
        <span className="progress__bar-fill" style={{ width: `${percent}%` }} />
      </div>
      <dl className="progress__stats track-progress__stats">
        <div>
          <dt>Cours terminés</dt>
          <dd>
            {progress.coursesCompleted}
            <span className="progress__total"> / {progress.courses}</span>
          </dd>
        </div>
        <div>
          <dt>Sections</dt>
          <dd>
            {progress.sectionsCompleted}
            <span className="progress__total"> / {progress.sections}</span>
          </dd>
        </div>
        <div>
          <dt>Temps travaillé</dt>
          <dd>{formatDuration(progress.minutesDone)}</dd>
        </div>
      </dl>
    </div>
  );
}
