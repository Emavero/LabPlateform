import { useEffect, useRef } from 'react';
import { useI18n } from '../i18n/I18nContext';

/**
 * Redemande les données au serveur lorsque la langue change.
 * <p>
 * Une partie des textes affichés ne vient pas du catalogue du navigateur mais
 * du serveur : noms de difficultés, natures d'actes du journal, signaux
 * d'exposition, statuts d'assistance. Ils arrivent déjà traduits, dans la
 * langue annoncée au moment de la requête — changer de langue ne les réécrit
 * donc pas, et la page se retrouverait moitié française, moitié anglaise.
 * <p>
 * Le premier rendu est volontairement ignoré : le chargement initial est déjà
 * fait par le crochet appelant, et le déclencher deux fois doublerait chaque
 * requête au démarrage.
 */
export function useLanguageRefresh(reload: () => void | Promise<unknown>): void {
  const { language } = useI18n();
  const previous = useRef(language);

  useEffect(() => {
    if (previous.current === language) {
      return;
    }
    previous.current = language;
    void reload();
  }, [language, reload]);
}
