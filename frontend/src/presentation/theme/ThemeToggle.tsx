import { THEMES } from '@/domain/models/Theme';
import { Icon } from '../design-system';
import { useI18n } from '../i18n/I18nContext';
import { useTheme } from './ThemeContext';

/**
 * Bascule de thème, en une icône, voisine de la bascule de langue.
 * <p>
 * Comme pour la langue : avec deux thèmes, un bouton unique suffit, et il
 * annonce celui vers lequel il mène. L'icône montre donc la destination — un
 * soleil quand on est dans le sombre — parce qu'un bouton qui affiche l'état
 * courant se lit à l'envers une fois sur deux.
 */
export function ThemeToggle() {
  const { theme, setTheme } = useTheme();
  const { t } = useI18n();
  const next = THEMES.find((candidate) => candidate !== theme) ?? 'dark';
  const label = t(next === 'light' ? 'a11y.switchToLight' : 'a11y.switchToDark');

  return (
    <button
      type="button"
      className="icon-btn theme-toggle"
      onClick={() => setTheme(next)}
      aria-label={label}
      title={label}
    >
      <Icon name={next === 'light' ? 'sun' : 'moon'} size={20} />
    </button>
  );
}
