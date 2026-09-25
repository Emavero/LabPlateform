import { Link } from 'react-router-dom';
import type { Box } from '@/domain/models/Box';
import { OS_FAMILY_LABELS } from '@/domain/models/VirtualMachine';
import { Icon } from '../../design-system';
import { DifficultyMeter } from './DifficultyMeter';
import { FlagChip } from './FlagChip';

/** Vignette du catalogue : ce qu'il faut pour choisir sa prochaine cible. */
export function BoxCard({ box }: { box: Box }) {
  const titleId = `box-${box.slug}-title`;
  return (
    <article className={['box-card', box.pwned && 'box-card--pwned'].filter(Boolean).join(' ')} aria-labelledby={titleId}>
      <header className="box-card__header">
        <span className={`box-card__os box-card__os--${box.os.toLowerCase()}`}>
          <Icon name={box.os === 'WINDOWS' ? 'windows' : 'linux'} size={24} />
        </span>
        <div className="box-card__heading">
          <p className="box-card__family">{OS_FAMILY_LABELS[box.os]}</p>
          <h3 className="box-card__name" id={titleId}>
            <Link to={`/machines/${box.slug}`}>{box.name}</Link>
          </h3>
        </div>
        <div className="box-card__badges">
          {box.pwned && (
            <span className="badge badge--pwned">
              <Icon name="check" size={13} /> Possédée
            </span>
          )}
          {box.firstBlood && (
            <span className="badge badge--blood">
              <Icon name="crown" size={13} /> First blood
            </span>
          )}
          {box.retired && <span className="badge">Retirée</span>}
        </div>
      </header>

      <p className="box-card__synopsis">{box.synopsis}</p>

      <div className="box-card__meta">
        <DifficultyMeter difficulty={box.difficulty} label={box.difficultyName} />
        <span className="box-card__points">
          {box.pointsEarned > 0 ? `${box.pointsEarned} / ${box.totalPoints}` : box.totalPoints} pts
        </span>
      </div>

      <div className="box-card__flags">
        <FlagChip kind="USER" owned={box.userOwned} points={box.userFlagPoints} />
        <FlagChip kind="ROOT" owned={box.rootOwned} points={box.rootFlagPoints} />
      </div>

      <footer className="box-card__footer">
        <code className="box-card__ip">{box.ipAddress}</code>
        <Link className="text-link" to={`/machines/${box.slug}`}>
          Attaquer <Icon name="chevronRight" size={14} />
        </Link>
      </footer>
    </article>
  );
}
