import { LANGUAGE_NAMES, LANGUAGES } from '@/domain/models/Language';
import { useI18n } from './I18nContext';

/**
 * Choix de la langue.
 * <p>
 * Chaque langue est écrite dans sa propre langue — « Français », « English » —
 * et non traduite : quelqu'un qui cherche l'anglais cherche le mot
 * « English », pas « Anglais ».
 */
export function LanguagePicker({ compact = false }: { compact?: boolean }) {
  const { language, setLanguage, t } = useI18n();

  return (
    <div
      className={['language-picker', compact && 'language-picker--compact'].filter(Boolean).join(' ')}
      role="group"
      aria-label={t('common.language')}
    >
      {LANGUAGES.map((candidate) => (
        <button
          key={candidate}
          type="button"
          lang={candidate}
          className={['language-picker__option', language === candidate && 'language-picker__option--active']
            .filter(Boolean)
            .join(' ')}
          aria-pressed={language === candidate}
          onClick={() => setLanguage(candidate)}
        >
          {compact ? candidate.toUpperCase() : LANGUAGE_NAMES[candidate]}
        </button>
      ))}
    </div>
  );
}
