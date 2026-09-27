import type { Role } from '@/domain/models/User';
import type { IconName } from '../design-system';
import type { CatalogueKey } from '../i18n/I18nContext';

export interface NavItem {
  /** Clé de traduction : le menu ne porte pas de texte, il porte des clés. */
  readonly label: CatalogueKey;
  readonly to: string;
  readonly icon: IconName;
  /** Correspondance exacte du chemin (pour la racine). */
  readonly end?: boolean;
  /** Rôles qui voient l'entrée. Absent : tout le monde la voit. */
  readonly roles?: readonly Role[];
}

/**
 * Entrée de menu à deux niveaux : un intitulé qui regroupe des sous-entrées.
 * Le groupe n'est pas cliquable, ses enfants le sont.
 */
export interface NavGroup {
  readonly label: CatalogueKey;
  readonly icon: IconName;
  readonly children: readonly NavItem[];
  readonly roles?: readonly Role[];
}

export type NavEntry = NavItem | NavGroup;

export function isGroup(entry: NavEntry): entry is NavGroup {
  return 'children' in entry;
}

/**
 * Menu tel que ce rôle le voit.
 * <p>
 * Le filtrage va dans les deux sens : l'administrateur ne voit pas les pages de
 * joueur, et le joueur ne voit pas celles d'administration. La séparation est
 * voulue — l'administrateur publie le contenu, il ne le consomme pas, et un
 * menu qui mélange les deux métiers n'aide personne.
 * <p>
 * Ce n'est pas une protection : c'est le serveur qui refuse /api/admin/** à
 * quiconque n'a pas le rôle.
 */
export function navigationFor(entries: readonly NavEntry[], role: Role | undefined): NavEntry[] {
  const allowed = (roles: readonly Role[] | undefined) => !roles || (role !== undefined && roles.includes(role));
  return entries
    .filter((entry) => allowed(entry.roles))
    .map((entry) =>
      isGroup(entry) ? { ...entry, children: entry.children.filter((child) => allowed(child.roles)) } : entry,
    )
    .filter((entry) => !isGroup(entry) || entry.children.length > 0);
}

export interface ModuleInfo {
  /** Clés de traduction : une page de module ne porte pas de texte. */
  readonly title: CatalogueKey;
  readonly description: CatalogueKey;
}

/**
 * Menu déclaratif : ajouter une entrée ne demande aucune modification de la
 * Sidebar (OCP). Les modules non encore livrés pointent vers une page d'attente.
 */
/** Rôles qui jouent : tout sauf l'administration. */
const PLAYER: readonly Role[] = ['USER'];

export const PRIMARY_NAV: readonly NavEntry[] = [
  { label: 'nav.dashboard', to: '/', icon: 'dashboard', end: true, roles: PLAYER },
  { label: 'nav.machines', to: '/machines', icon: 'target', roles: PLAYER },
  { label: 'nav.scoreboard', to: '/scoreboard', icon: 'trophy', roles: PLAYER },
  {
    label: 'nav.courses',
    icon: 'book',
    roles: PLAYER,
    children: [
      { label: 'nav.forensics', to: '/cours/forensique', icon: 'search' },
      { label: 'nav.defense', to: '/cours/defense', icon: 'shield' },
    ],
  },
  { label: 'nav.labs', to: '/labs', icon: 'server', roles: PLAYER },
  { label: 'nav.vpn', to: '/vpn', icon: 'vpn', roles: PLAYER },
  { label: 'nav.exposure', to: '/modules/exposure-analysis', icon: 'radar', roles: PLAYER },
  { label: 'nav.attackPaths', to: '/modules/attack-paths', icon: 'route', roles: PLAYER },
  { label: 'nav.events', to: '/modules/events', icon: 'activity', roles: PLAYER },
  { label: 'nav.scenarios', to: '/modules/scenario-designer', icon: 'scenario', roles: PLAYER },
  { label: 'nav.reports', to: '/modules/report-center', icon: 'report', roles: PLAYER },
  {
    label: 'nav.administration',
    icon: 'admin',
    roles: ['ADMIN'],
    children: [
      { label: 'nav.dashboard', to: '/admin', icon: 'dashboard', end: true },
      { label: 'nav.adminCourses', to: '/admin/cours', icon: 'book' },
      { label: 'nav.adminMachines', to: '/admin/machines', icon: 'target' },
      { label: 'nav.support', to: '/admin/support', icon: 'support' },
    ],
  },
];

export const SECONDARY_NAV: readonly NavEntry[] = [
  { label: 'nav.profile', to: '/profil', icon: 'medal', roles: PLAYER },
  { label: 'nav.subscription', to: '/abonnement', icon: 'crown', roles: PLAYER },
  // Les réglages du compte (mot de passe, langue) valent pour les deux rôles.
  { label: 'nav.settings', to: '/settings', icon: 'settings' },
  { label: 'nav.support', to: '/modules/support', icon: 'support', roles: PLAYER },
];

/**
 * Filières de cours, indexées par le segment d'URL. Miroir de
 * domain/academy/Track côté serveur : ajouter une filière ici et dans
 * PRIMARY_NAV suffit à la faire apparaître.
 */
export const TRACK_PAGES: Readonly<Record<string, ModuleInfo>> = {
  forensique: { title: 'track.FORENSICS', description: 'track.forensique.description' },
  defense: { title: 'track.DEFENSE', description: 'track.defense.description' },
};

export const MODULES: Readonly<Record<string, ModuleInfo>> = {
  'scenario-designer': {
    title: 'module.scenario-designer.title',
    description: 'module.scenario-designer.description',
  },
};
