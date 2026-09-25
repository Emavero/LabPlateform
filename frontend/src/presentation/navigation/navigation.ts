import type { IconName } from '../design-system';

export interface NavItem {
  readonly label: string;
  readonly to: string;
  readonly icon: IconName;
  /** Correspondance exacte du chemin (pour la racine). */
  readonly end?: boolean;
}

export interface ModuleInfo {
  readonly title: string;
  readonly description: string;
}

/**
 * Menu déclaratif : ajouter une entrée ne demande aucune modification de la
 * Sidebar (OCP). Les modules non encore livrés pointent vers une page d'attente.
 */
export const PRIMARY_NAV: readonly NavItem[] = [
  { label: 'Dashboard', to: '/', icon: 'dashboard', end: true },
  { label: 'Machines', to: '/machines', icon: 'target' },
  { label: 'Classement', to: '/scoreboard', icon: 'trophy' },
  { label: 'Lab Infrastructure', to: '/labs', icon: 'server' },
  { label: 'VPN Access', to: '/vpn', icon: 'vpn' },
  { label: 'Exposure Analysis', to: '/modules/exposure-analysis', icon: 'radar' },
  { label: 'Attack Paths', to: '/modules/attack-paths', icon: 'route' },
  { label: 'Events', to: '/modules/events', icon: 'activity' },
  { label: 'Scenario Designer', to: '/modules/scenario-designer', icon: 'scenario' },
  { label: 'Administration', to: '/modules/administration', icon: 'admin' },
  { label: 'Report Center', to: '/modules/report-center', icon: 'report' },
];

export const SECONDARY_NAV: readonly NavItem[] = [
  { label: 'Settings', to: '/settings', icon: 'settings' },
  { label: 'Support', to: '/modules/support', icon: 'support' },
];

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
  administration: {
    title: 'Administration',
    description: 'Gestion des utilisateurs, des rôles et des modèles de machines.',
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
