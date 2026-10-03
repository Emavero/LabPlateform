import type { CourseSummary, Topic, Track } from './Course';

/**
 * Ce que l'apprenant a choisi de voir : une filière, puis éventuellement un de
 * ses sous-domaines. Les deux peuvent être absents — c'est alors tout le
 * catalogue.
 */
export interface CatalogueFilter {
  /** Slug de la filière, ou null pour toutes. */
  readonly trackSlug: string | null;
  /** Slug du sous-domaine, ou null pour tous ceux de la filière. */
  readonly topicSlug: string | null;
}

export const NO_FILTER: CatalogueFilter = { trackSlug: null, topicSlug: null };

/**
 * Cours retenus par le filtre.
 * <p>
 * Le filtrage se fait ici, sur la liste déjà chargée, et non par un aller-retour
 * au serveur : choisir un sous-domaine doit répondre au clic, et le catalogue
 * entier tient largement en mémoire.
 */
export function filterCourses(
  courses: readonly CourseSummary[],
  filter: CatalogueFilter,
): CourseSummary[] {
  return courses.filter(
    (course) =>
      (filter.trackSlug === null || course.trackSlug === filter.trackSlug) &&
      (filter.topicSlug === null || course.topicSlug === filter.topicSlug),
  );
}

/**
 * Sous-domaines à proposer pour la filière choisie, et leur nombre de cours.
 * <p>
 * Un sous-domaine sans aucun cours est écarté : un filtre qui ne peut mener
 * qu'à une page vide n'aide personne à choisir. La liste vient des filières
 * décrites par le serveur, pas des cours — leur ordre reste donc celui voulu,
 * même si un sous-domaine n'a qu'un seul cours.
 */
export function topicsOf(
  tracks: readonly Track[],
  courses: readonly CourseSummary[],
  trackSlug: string | null,
): { topic: Topic; courses: number }[] {
  if (trackSlug === null) return [];
  const track = tracks.find((candidate) => candidate.slug === trackSlug);
  if (!track) return [];
  return track.topics
    .map((topic) => ({
      topic,
      courses: courses.filter((course) => course.topicSlug === topic.slug).length,
    }))
    .filter((entry) => entry.courses > 0);
}

/**
 * Sous-domaine à retenir quand la filière change.
 * <p>
 * Un sous-domaine de Forensique n'a pas de sens sous Défense : il est alors
 * oublié, plutôt que de laisser l'apprenant devant une liste vide sans
 * comprendre pourquoi.
 */
export function topicStillValid(tracks: readonly Track[], trackSlug: string | null, topicSlug: string | null): boolean {
  if (topicSlug === null) return true;
  if (trackSlug === null) return false;
  return (tracks.find((track) => track.slug === trackSlug)?.topics ?? []).some(
    (topic) => topic.slug === topicSlug,
  );
}
