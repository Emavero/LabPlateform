import type { Role } from '@/domain/models/User';
import type { IconName } from '../design-system';

export interface NavItem {
  readonly label: string;
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
  readonly label: string;
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
  readonly title: string;
  readonly description: string;
}

/**
 * Menu déclaratif : ajouter une entrée ne demande aucune modification de la
 * Sidebar (OCP). Les modules non encore livrés pointent vers une page d'attente.
 */
/** Rôles qui jouent : tout sauf l'administration. */
const PLAYER: readonly Role[] = ['USER'];

export const PRIMARY_NAV: readonly NavEntry[] = [
  { label: 'Dashboard', to: '/', icon: 'dashboard', end: true, roles: PLAYER },
  { label: 'Machines', to: '/machines', icon: 'target', roles: PLAYER },
  { label: 'Classement', to: '/scoreboard', icon: 'trophy', roles: PLAYER },
  {
    label: 'Cours',
    icon: 'book',
    roles: PLAYER,
    children: [
      { label: 'Forensique', to: '/cours/forensique', icon: 'search' },
      { label: 'Défense', to: '/cours/defense', icon: 'shield' },
    ],
  },
  { label: 'Lab Infrastructure', to: '/labs', icon: 'server', roles: PLAYER },
  { label: 'VPN Access', to: '/vpn', icon: 'vpn', roles: PLAYER },
  { label: 'Exposure Analysis', to: '/modules/exposure-analysis', icon: 'radar', roles: PLAYER },
  { label: 'Attack Paths', to: '/modules/attack-paths', icon: 'route', roles: PLAYER },
  { label: 'Events', to: '/modules/events', icon: 'activity', roles: PLAYER },
  { label: 'Scenario Designer', to: '/modules/scenario-designer', icon: 'scenario', roles: PLAYER },
  { label: 'Report Center', to: '/modules/report-center', icon: 'report', roles: PLAYER },
  {
    label: 'Administration',
    icon: 'admin',
    roles: ['ADMIN'],
    children: [
      { label: 'Tableau de bord', to: '/admin', icon: 'dashboard', end: true },
      { label: 'Gérer les cours', to: '/admin/cours', icon: 'book' },
      { label: 'Gérer les machines', to: '/admin/machines', icon: 'target' },
    ],
  },
];

export const SECONDARY_NAV: readonly NavEntry[] = [
  { label: 'Profil', to: '/profil', icon: 'medal', roles: PLAYER },
  { label: 'Abonnement', to: '/abonnement', icon: 'crown', roles: PLAYER },
  // Les réglages du compte (mot de passe, langue) valent pour les deux rôles.
  { label: 'Settings', to: '/settings', icon: 'settings' },
  { label: 'Support', to: '/modules/support', icon: 'support', roles: PLAYER },
];

/**
 * Filières de cours, indexées par le segment d'URL. Miroir de
 * domain/academy/Track côté serveur : ajouter une filière ici et dans
 * PRIMARY_NAV suffit à la faire apparaître.
 */
export const TRACK_PAGES: Readonly<Record<string, ModuleInfo>> = {
  forensique: {
    title: 'Forensique',
    description:
      'Analyse post-incident : collecte de traces, mémoire, disques, journaux, chronologie.',
  },
  defense: {
    title: 'Défense',
    description: 'Durcissement, détection et réponse : surveiller, contenir et fermer les portes.',
  },
};

export const MODULES: Readonly<Record<string, ModuleInfo>> = {
  'exposure-analysis': {
    title: 'Exposure Analysis',
    description: "Cartographie de la surface d'attaque exposée par vos environnements de lab.",
  },
  'attack-paths': {
    title: 'Attack Paths',
    description: "Visualisation des chemins d'attaque possibles entre les machines du lab.",
  },
  events: {
    title: 'Events',
    description: 'Journal centralisé des événements de sécurité remontés par les machines.',
  },
  'scenario-designer': {
    title: 'Scenario Designer',
    description: "Conception de scénarios d'exercice rejouables sur l'infrastructure du lab.",
  },
  'report-center': {
    title: 'Report Center',
    description: 'Rapports d’activité et de progression générés à partir de vos sessions.',
  },
  support: {
    title: 'Support',
    description: "Documentation, FAQ et contact de l'équipe plateforme.",
  },
};
