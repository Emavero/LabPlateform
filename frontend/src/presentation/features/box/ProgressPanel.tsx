import { completionPercent, progressToNextRank, type PlayerProgress } from '@/domain/models/Progress';
import { Icon } from '../../design-system';

/** Rang, points et avancement : la carte d'identité du joueur. */
export function ProgressPanel({ progress }: { progress: PlayerProgress }) {
  const percent = completionPercent(progress);
  const toNext = Math.round(progressToNextRank(progress) * 100);

  return (
    <div className="progress">
      <div className="progress__rank">
        <span className="progress__crown">
          <Icon name="crown" size={22} />
        </span>
        <div>
          <p className="progress__rank-label">Rang</p>
          <p className="progress__rank-name">{progress.rankName}</p>
        </div>
      </div>

      <div className="progress__bar-block">
        <div className="progress__bar" role="img" aria-label={`${percent} % du catalogue possédé`}>
          <span className="progress__bar-fill" style={{ width: `${toNext}%` }} />
        </div>
        <p className="progress__hint">
          {progress.nextRankName
            ? `${progress.pointsToNextRank} point${progress.pointsToNextRank > 1 ? 's' : ''} avant ${progress.nextRankName}`
            : 'Rang maximal atteint : tout le catalogue est à vous.'}
        </p>
      </div>

      <dl className="progress__stats">
        <div>
          <dt>Points</dt>
          <dd>
            {progress.points}
            <span className="progress__total"> / {progress.availablePoints}</span>
          </dd>
        </div>
        <div>
          <dt>Flags</dt>
          <dd>
            {progress.ownedFlags}
            <span className="progress__total"> / {progress.totalFlags}</span>
          </dd>
        </div>
        <div>
          <dt>Machines</dt>
          <dd>{progress.boxesPwned}</dd>
        </div>
        <div>
          <dt>First bloods</dt>
          <dd>{progress.firstBloods}</dd>
        </div>
      </dl>
    </div>
  );
}
