import { completionPercent, progressToNextRank, type PlayerProgress } from '@/domain/models/Progress';
import { Icon } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';

/** Rang, points et avancement : la carte d'identité du joueur. */
export function ProgressPanel({ progress }: { progress: PlayerProgress }) {
  const { t } = useI18n();
  const percent = completionPercent(progress);
  const toNext = Math.round(progressToNextRank(progress) * 100);

  return (
    <div className="progress">
      <div className="progress__rank">
        <span className="progress__crown">
          <Icon name="crown" size={22} />
        </span>
        <div>
          <p className="progress__rank-label">{t('progress.rank')}</p>
          <p className="progress__rank-name">{progress.rankName}</p>
        </div>
      </div>

      <div className="progress__bar-block">
        <div className="progress__bar" role="img" aria-label={t('progress.percentOwned', { percent })}>
          <span className="progress__bar-fill" style={{ width: `${toNext}%` }} />
        </div>
        <p className="progress__hint">
          {progress.nextRankName
            ? t('progress.toNextRank', { points: progress.pointsToNextRank, rank: progress.nextRankName })
            : t('progress.maxRank')}
        </p>
      </div>

      <dl className="progress__stats">
        <div>
          <dt>{t('progress.points')}</dt>
          <dd>
            {progress.points}
            <span className="progress__total"> / {progress.availablePoints}</span>
          </dd>
        </div>
        <div>
          <dt>{t('progress.flags')}</dt>
          <dd>
            {progress.ownedFlags}
            <span className="progress__total"> / {progress.totalFlags}</span>
          </dd>
        </div>
        <div>
          <dt>{t('progress.machines')}</dt>
          <dd>{progress.boxesPwned}</dd>
        </div>
        <div>
          <dt>{t('progress.firstBloods')}</dt>
          <dd>{progress.firstBloods}</dd>
        </div>
      </dl>
    </div>
  );
}
