import { describe, expect, it } from 'vitest';
import { isGroup, navigationFor, PRIMARY_NAV, SECONDARY_NAV, type NavEntry } from './navigation';

function labels(entries: NavEntry[]): string[] {
  return entries.map((entry) => entry.label);
}

describe('navigationFor', () => {
  it('ne montre au joueur aucune entrée d’administration', () => {
    const menu = labels(navigationFor(PRIMARY_NAV, 'USER'));

    expect(menu).toContain('Machines');
    expect(menu).toContain('Cours');
    expect(menu).not.toContain('Administration');
  });

  /** L'administrateur publie le contenu : le catalogue et le classement ne le concernent pas. */
  it('ne montre à l’administrateur que l’administration', () => {
    const menu = labels(navigationFor(PRIMARY_NAV, 'ADMIN'));

    expect(menu).toEqual(['Administration']);
  });

  it('garde les réglages du compte pour les deux rôles, et l’abonnement pour le joueur seul', () => {
    expect(labels(navigationFor(SECONDARY_NAV, 'ADMIN'))).toEqual(['Settings']);
    expect(labels(navigationFor(SECONDARY_NAV, 'USER'))).toContain('Abonnement');
  });

  it('conserve les sous-entrées des groupes visibles', () => {
    const admin = navigationFor(PRIMARY_NAV, 'ADMIN')[0];

    expect(isGroup(admin)).toBe(true);
    if (isGroup(admin)) {
      expect(admin.children.map((child) => child.to)).toEqual(['/admin', '/admin/cours', '/admin/machines']);
    }
  });

  /** Sans rôle connu (session en cours de restauration), rien de réservé ne fuit. */
  it('ne montre rien de réservé à un rôle inconnu', () => {
    expect(labels(navigationFor(PRIMARY_NAV, undefined))).toEqual([]);
  });
});
