import type { OutlineEntry } from '@/domain/models/CourseOutline';
import { useI18n } from '../../i18n/I18nContext';
import { useActiveAnchor } from '../../hooks/useActiveAnchor';

/**
 * Sommaire « Sur cette page ».
 * <p>
 * Ce sont des liens d'ancre ordinaires, pas des boutons qui feraient défiler en
 * script : un lien se copie, s'ouvre dans un onglet, et le clavier le connaît
 * déjà. Le défilement doux est laissé à la feuille de style, qui sait le
 * désactiver pour qui demande moins d'animations.
 */
export function CourseOutlinePanel({ entries }: { entries: readonly OutlineEntry[] }) {
  const { t } = useI18n();
  const active = useActiveAnchor(entries.map((entry) => entry.id));

  if (entries.length === 0) return null;

  return (
    <nav className="course-toc" aria-labelledby="course-toc-title">
      <p className="course-toc__title" id="course-toc-title">
        {t('course.onThisPage')}
      </p>
      <ul className="course-toc__list">
        {entries.map((entry) => (
          <li key={entry.id}>
            <a
              className={[
                'course-toc__link',
                entry.level === 3 && 'course-toc__link--sub',
                entry.id === active && 'course-toc__link--active',
              ]
                .filter(Boolean)
                .join(' ')}
              href={`#${entry.id}`}
              aria-current={entry.id === active ? 'location' : undefined}
            >
              {entry.label}
            </a>
          </li>
        ))}
      </ul>
    </nav>
  );
}
