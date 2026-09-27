import { Link, useParams } from 'react-router-dom';
import { remainingFlags, type FlagKind } from '@/domain/models/Box';
import { Alert, Button, Icon, Panel, Spinner } from '../design-system';
import { DifficultyMeter } from '../features/box/DifficultyMeter';
import { FlagForm, FlagSuccess } from '../features/box/FlagForm';
import { FlagChip } from '../features/box/FlagChip';
import { PaywallPanel } from '../features/billing/PaywallPanel';
import { InstancePanel } from '../features/box/InstancePanel';
import { RatingPicker } from '../features/box/RatingPicker';
import { Writeups } from '../features/box/Writeups';
import { useI18n } from '../i18n/I18nContext';
import { useBoxDetail } from '../hooks/useBoxDetail';
import { NotFoundPage } from './NotFoundPage';

/** Fiche d'une machine : contexte, adresse à attaquer, soumission des flags. */
export function BoxDetailPage() {
  const { t, formatDate } = useI18n();
  const { slug = '' } = useParams();
  const detail = useBoxDetail(slug);

  if (detail.loading && !detail.box) {
    return (
      <div className="page">
        <div className="empty">
          <Spinner size={22} label={t('boxes.loading')} />
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
          title={t('boxes.loadError')}
          action={
            <Button variant="ghost" size="sm" icon="refresh" onClick={() => void detail.reload()}>
              {t('common.retry')}
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
        <Icon name="chevronRight" size={14} /> {t('boxes.catalogue')}
      </Link>

      <header className="page__header">
        <div>
          <p className="page__eyebrow">
            {t(`os.${box.os}`)} · {box.osName} · {t('boxes.by', { maker: box.maker })}
          </p>
          <h1 className="page__title">{box.name}</h1>
          <p className="page__lead">{box.locked ? t('boxes.lockedLead') : box.synopsis}</p>
        </div>
        <div className="box-detail__badges">
          <DifficultyMeter difficulty={box.difficulty} label={box.difficultyName} />
          {box.locked && (
            <span className="badge badge--locked">
              <Icon name="lock" size={13} /> {t('boxes.lockedBadge')}
            </span>
          )}
          {box.pwned && (
            <span className="badge badge--pwned">
              <Icon name="check" size={13} /> {t('boxes.owned')}
            </span>
          )}
          {box.firstBlood && (
            <span className="badge badge--blood">
              <Icon name="crown" size={13} /> {t('boxes.firstBlood')}
            </span>
          )}
        </div>
      </header>

      {box.locked ? (
        <div className="detail-grid">
          <PaywallPanel box={box} />
          <Panel title={t('boxes.preview')} description={t('boxes.previewHint')}>
            <dl className="box-detail__facts">
              <div>
                <dt>{t('boxes.difficulty')}</dt>
                <dd>{box.difficultyName}</dd>
              </div>
              <div>
                <dt>{t('boxes.points')}</dt>
                <dd>{box.totalPoints}</dd>
              </div>
              <div>
                <dt>{t('boxes.releasedOn')}</dt>
                <dd>{formatDate(box.releasedAt)}</dd>
              </div>
              <div>
                <dt>{t('boxes.perceived')}</dt>
                <dd>{box.perceivedDifficultyName ?? t('boxes.notRated')}</dd>
              </div>
            </dl>
          </Panel>
        </div>
      ) : (
      <div className="detail-grid">
        <Panel title={t('instance.title')} description={t('instance.hint')}>
          <InstancePanel box={box} pending={detail.instancePending} onToggle={() => void detail.toggleInstance()} />
          <dl className="box-detail__facts">
            <div>
              <dt>{t('boxes.difficulty')}</dt>
              <dd>{box.difficultyName}</dd>
            </div>
            <div>
              <dt>{t('boxes.points')}</dt>
              <dd>
                {box.pointsEarned} / {box.totalPoints}
              </dd>
            </div>
            <div>
              <dt>{t('boxes.releasedOn')}</dt>
              <dd>{formatDate(box.releasedAt)}</dd>
            </div>
            <div>
              <dt>{t('boxes.state')}</dt>
              <dd>{t(box.retired ? 'boxes.retired' : 'boxes.active')}</dd>
            </div>
          </dl>
          <div className="box-card__flags">
            <FlagChip kind="USER" owned={box.userOwned} points={box.userFlagPoints} />
            <FlagChip kind="ROOT" owned={box.rootOwned} points={box.rootFlagPoints} />
          </div>
        </Panel>

        <Panel title={t('rating.title')} description={t('rating.hint')}>
          <RatingPicker box={box} pending={detail.rating} onRate={(difficulty) => void detail.rate(difficulty)} />
        </Panel>

        <Panel
          title={t('flag.submitTitle')}
          description={t(box.pwned ? 'flag.submitHintDone' : 'flag.submitHint')}
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
            <p className="empty">{t('flag.allDone')}</p>
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
      )}

      {!box.locked && <Writeups slug={box.slug} pwned={box.pwned} />}
    </div>
  );
}
