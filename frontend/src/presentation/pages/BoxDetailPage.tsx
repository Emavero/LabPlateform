import { Link, useParams } from 'react-router-dom';
import { remainingFlags, type FlagKind } from '@/domain/models/Box';
import { OS_FAMILY_LABELS } from '@/domain/models/VirtualMachine';
import { Alert, Button, CopyField, Icon, Panel, Spinner } from '../design-system';
import { DifficultyMeter } from '../features/box/DifficultyMeter';
import { FlagForm, FlagSuccess } from '../features/box/FlagForm';
import { FlagChip } from '../features/box/FlagChip';
import { RatingPicker } from '../features/box/RatingPicker';
import { useBoxDetail } from '../hooks/useBoxDetail';
import { NotFoundPage } from './NotFoundPage';

/** Fiche d'une machine : contexte, adresse à attaquer, soumission des flags. */
export function BoxDetailPage() {
  const { slug = '' } = useParams();
  const detail = useBoxDetail(slug);

  if (detail.loading && !detail.box) {
    return (
      <div className="page">
        <div className="empty">
          <Spinner size={22} label="Chargement de la machine" />
        </div>
      </div>
    );
  }
  if (detail.error?.kind === 'not_found') return <NotFoundPage />;
  if (detail.error || !detail.box) {
    return (
      <div className="page">
        <Alert
          tone="error"
          title="Impossible de charger cette machine"
          action={
            <Button variant="ghost" size="sm" icon="refresh" onClick={() => void detail.reload()}>
              Réessayer
            </Button>
          }
        >
          {detail.error?.message}
        </Alert>
      </div>
    );
  }

  const box = detail.box;
  const submit = (kind: FlagKind, flag: string) => detail.submit(kind, flag);

  return (
    <div className="page">
      <Link className="back-link" to="/machines">
        <Icon name="chevronRight" size={14} /> Catalogue
      </Link>

      <header className="page__header">
        <div>
          <p className="page__eyebrow">
            {OS_FAMILY_LABELS[box.os]} · {box.osName} · par {box.maker}
          </p>
          <h1 className="page__title">{box.name}</h1>
          <p className="page__lead">{box.synopsis}</p>
        </div>
        <div className="box-detail__badges">
          <DifficultyMeter difficulty={box.difficulty} label={box.difficultyName} />
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
        </div>
      </header>

      <div className="detail-grid">
        <Panel title="Cible" description="Joignable une fois le VPN du lab monté.">
          <CopyField label="Adresse" value={box.ipAddress} />
          <dl className="box-detail__facts">
            <div>
              <dt>Difficulté</dt>
              <dd>{box.difficultyName}</dd>
            </div>
            <div>
              <dt>Points</dt>
              <dd>
                {box.pointsEarned} / {box.totalPoints}
              </dd>
            </div>
            <div>
              <dt>Publiée le</dt>
              <dd>{box.releasedAt.toLocaleDateString('fr-FR')}</dd>
            </div>
            <div>
              <dt>État</dt>
              <dd>{box.retired ? 'Retirée' : 'Active'}</dd>
            </div>
          </dl>
          <div className="box-card__flags">
            <FlagChip kind="USER" owned={box.userOwned} points={box.userFlagPoints} />
            <FlagChip kind="ROOT" owned={box.rootOwned} points={box.rootFlagPoints} />
          </div>
        </Panel>

        <Panel title="Difficulté ressentie" description="Ce qu'en disent les joueurs qui l'ont faite.">
          <RatingPicker box={box} pending={detail.rating} onRate={(difficulty) => void detail.rate(difficulty)} />
        </Panel>

        <Panel
          title="Soumettre un flag"
          description={
            box.pwned
              ? 'Les deux flags sont validés : cette machine ne rapporte plus de points.'
              : 'Collez le flag trouvé sur la machine. Chaque flag ne compte qu’une fois.'
          }
        >
          {detail.lastSubmission && (
            <FlagSuccess
              kind={detail.lastSubmission.kind}
              points={detail.lastSubmission.pointsAwarded}
              firstBlood={detail.lastSubmission.firstBlood}
              pwned={detail.lastSubmission.pwned}
            />
          )}
          {detail.submitError && <Alert tone="error">{detail.submitError.message}</Alert>}

          {remainingFlags(box).length === 0 ? (
            <p className="empty">Machine terminée. Choisissez votre prochaine cible dans le catalogue.</p>
          ) : (
            (['USER', 'ROOT'] as const).map((kind) => (
              <FlagForm
                key={kind}
                kind={kind}
                points={kind === 'USER' ? box.userFlagPoints : box.rootFlagPoints}
                owned={kind === 'USER' ? box.userOwned : box.rootOwned}
                submitting={detail.submitting === kind}
                onSubmit={submit}
              />
            ))
          )}
        </Panel>
      </div>
    </div>
  );
}
