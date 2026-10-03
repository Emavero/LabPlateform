import { useEffect, useState } from 'react';

/**
 * Titre en cours de lecture, parmi ceux dont on donne les ancres.
 * <p>
 * L'observateur d'intersection porte le cas courant : il ne réveille la page que
 * lorsqu'un titre entre ou sort, là où un gestionnaire de `scroll` s'exécuterait
 * des dizaines de fois par seconde pour le même résultat.
 * <p>
 * La marge haute écarte la zone cachée par la barre supérieure : sans elle, le
 * titre surligné serait celui passé sous l'en-tête plutôt que celui qu'on lit.
 * Quand plusieurs titres sont visibles, le plus haut gagne — c'est celui sous
 * lequel on se trouve.
 * <p>
 * Reste le bas de page, que l'observateur ne sait pas traiter : les derniers
 * titres n'atteignent jamais la bande de lecture, parce qu'il n'y a plus assez
 * de contenu sous eux pour les y faire monter. Sans cela, cliquer « Concepteurs
 * du scénario » dans le sommaire y conduisait bien, mais surlignait une autre
 * entrée. D'où l'écoute du défilement, qui ne sert qu'à reconnaître la fin de la
 * page.
 */
export function useActiveAnchor(ids: readonly string[]): string | null {
  const [active, setActive] = useState<string | null>(null);
  // Les identifiants changent d'objet à chaque rendu, pas de contenu : la clé
  // évite de reconstruire l'observateur pour rien.
  const key = ids.join('|');

  useEffect(() => {
    const anchors = key.split('|').filter(Boolean);
    if (anchors.length === 0) {
      setActive(null);
      return;
    }
    // Environnement sans observateur (rendu côté serveur, test) : le sommaire
    // reste utilisable, simplement sans surlignage.
    if (typeof IntersectionObserver === 'undefined') {
      setActive(anchors[0]);
      return;
    }

    const visible = new Set<string>();

    const atBottom = () =>
      window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 2;

    const pick = () => {
      // En bas de page, c'est la dernière entrée qu'on lit, quelle que soit sa
      // position à l'écran : rien ne viendra plus la pousser vers le haut.
      if (atBottom()) {
        setActive(anchors[anchors.length - 1]);
        return;
      }
      const first = anchors.find((id) => visible.has(id));
      // Aucun titre visible (on est entre deux) : le dernier surlignage tient.
      if (first) setActive(first);
    };

    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) visible.add(entry.target.id);
          else visible.delete(entry.target.id);
        }
        pick();
      },
      { rootMargin: '-72px 0px -55% 0px' },
    );

    anchors
      .map((id) => document.getElementById(id))
      .filter((element): element is HTMLElement => element !== null)
      .forEach((element) => observer.observe(element));
    setActive((current) => current ?? anchors[0]);
    window.addEventListener('scroll', pick, { passive: true });

    return () => {
      observer.disconnect();
      window.removeEventListener('scroll', pick);
    };
  }, [key]);

  return active;
}
