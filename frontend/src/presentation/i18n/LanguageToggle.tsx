import { DEFAULT_LANGUAGE, LANGUAGES, LANGUAGE_NAMES } from '@/domain/models/Language';
import { Icon } from '../design-system';
import { useI18n } from './I18nContext';

/**
 * Bascule de langue, en une icône.
 * <p>
 * Avec deux langues, un bouton unique suffit : il annonce celle vers laquelle
 * il mène, pas celle en cours — la langue en cours, c'est la page qui la dit.
 * Un sélecteur à deux options coûterait deux fois la place pour la même
 * décision. Le libellé accessible nomme la langue cible, de sorte qu'un
 * lecteur d'écran annonce « Passer en English » et non « bouton globe ».
 * <p>
 * Le jour où une troisième langue arrive, ce bouton doit redevenir le
 * sélecteur complet, qui vit toujours dans les réglages.
 */
export function LanguageToggle() {
  const { language, setLanguage, t } = useI18n();
  const next = LANGUAGES.find((candidate) => candidate !== language) ?? DEFAULT_LANGUAGE;
  const label = t('a11y.switchLanguage', { language: LANGUAGE_NAMES[next] });

  return (
    <button
      type="button"
      className="icon-btn language-toggle"
      onClick={() => setLanguage(next)}
      aria-label={label}
      title={label}
      lang={next}
    >
      <Icon name="globe" size={20} />
      <span className="language-toggle__code" aria-hidden="true">
        {next.toUpperCase()}
      </span>
    </button>
  );
}
