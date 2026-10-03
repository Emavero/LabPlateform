import type { AttackPath } from '@/domain/models/Course';
import { Icon, Panel } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';

/**
 * Chemin d'attaque : le vecteur de menace, puis la chaîne, étape par étape.
 * <p>
 * L'illustration est la liste elle-même, ordonnée et reliée : une figure
 * dessinée dirait la même chose, mais ne se lirait ni au lecteur d'écran, ni sur
 * un téléphone, ni dans la langue de celui qui lit.
 */
export function AttackPathPanel({ id, path }: { id: string; path: AttackPath }) {
  const { t } = useI18n();

  return (
    <Panel
      id={id}
      className="attack-chain"
      eyebrow={t('course.attackPathEyebrow')}
      title={t('course.attackPath')}
      description={path.summary}
    >
      <ol className="attack-chain__list">
        {path.stages.map((stage) => (
          <li className="attack-chain__step" key={stage.position}>
            <span className="attack-chain__marker" aria-hidden="true">
              {stage.position}
            </span>
            <div className="attack-chain__body">
              <h3 className="attack-chain__name">{stage.name}</h3>
              <p className="attack-chain__description">{stage.description}</p>
              {stage.technique && (
                <p className="attack-chain__technique">
                  <Icon name="target" size={13} /> {stage.technique}
                </p>
              )}
            </div>
          </li>
        ))}
      </ol>
    </Panel>
  );
}
