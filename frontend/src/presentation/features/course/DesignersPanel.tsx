import type { CourseDesigner } from '@/domain/models/Course';
import { Panel } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';

/**
 * Concepteurs du scénario : qui a écrit ce cours, et à quel titre.
 * <p>
 * L'avatar est décoratif : le nom est écrit à côté, donc l'image ne porte pas
 * de texte alternatif — le répéter ferait entendre deux fois la même chose. Sans
 * photo, les initiales tiennent la place, plutôt qu'une silhouette générique qui
 * n'apprend rien.
 */
export function DesignersPanel({ id, designers }: { id: string; designers: readonly CourseDesigner[] }) {
  const { t } = useI18n();

  return (
    <Panel
      id={id}
      className="designers"
      eyebrow={t('course.designersEyebrow')}
      title={t('course.designers')}
    >
      <ul className="designers__list">
        {designers.map((designer) => (
          <li className="designer" key={`${designer.name}-${designer.role}`}>
            {designer.avatarUrl ? (
              <img className="designer__avatar" src={designer.avatarUrl} alt="" loading="lazy" />
            ) : (
              <span className="designer__avatar designer__avatar--initials" aria-hidden="true">
                {designer.initials}
              </span>
            )}
            <div>
              <p className="designer__name">{designer.name}</p>
              <p className="designer__role">{designer.role}</p>
            </div>
          </li>
        ))}
      </ul>
    </Panel>
  );
}
