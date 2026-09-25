export type Role = 'USER' | 'ADMIN';

export interface User {
  readonly id: number;
  readonly email: string;
  readonly role: Role;
}

export interface UserProfile extends User {
  readonly createdAt: Date;
}

/** Nom affiché : la partie locale de l'e-mail, faute de nom renseigné. */
export function displayNameOf(user: Pick<User, 'email'>): string {
  return user.email.split('@')[0] ?? user.email;
}

export function initialsOf(user: Pick<User, 'email'>): string {
  const parts = displayNameOf(user).split(/[._-]+/).filter(Boolean);
  const letters = parts.length >= 2 ? parts[0][0] + parts[1][0] : displayNameOf(user).slice(0, 2);
  return letters.toUpperCase();
}

export const ROLE_LABELS: Record<Role, string> = {
  USER: 'Analyste',
  ADMIN: 'Administrateur',
};
