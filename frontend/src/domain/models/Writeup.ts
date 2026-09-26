/**
 * Compte rendu de compromission. Publier ne le rend pas public : il devient
 * lisible par les joueurs qui ont eux aussi possédé la machine.
 */
export interface Writeup {
  readonly handle: string;
  /** Vrai si le lecteur en est l'auteur. */
  readonly mine: boolean;
  readonly title: string;
  readonly content: string;
  readonly published: boolean;
  readonly createdAt: Date;
  readonly updatedAt: Date;
}

export interface WriteupDraft {
  readonly title: string;
  readonly content: string;
  readonly published: boolean;
}

export const EMPTY_WRITEUP: WriteupDraft = { title: '', content: '', published: false };

export const WRITEUP_TITLE_MAX = 128;
export const WRITEUP_CONTENT_MAX = 40_000;

export function validateWriteup(draft: WriteupDraft): Partial<Record<'title' | 'content', string>> {
  return {
    title: !draft.title.trim()
      ? 'Le titre est obligatoire.'
      : draft.title.length > WRITEUP_TITLE_MAX
        ? `Le titre est limité à ${WRITEUP_TITLE_MAX} caractères.`
        : undefined,
    content: !draft.content.trim()
      ? 'Le compte rendu est vide.'
      : draft.content.length > WRITEUP_CONTENT_MAX
        ? `Le compte rendu est limité à ${WRITEUP_CONTENT_MAX} caractères.`
        : undefined,
  };
}

export function myWriteup(writeups: readonly Writeup[]): Writeup | undefined {
  return writeups.find((writeup) => writeup.mine);
}
