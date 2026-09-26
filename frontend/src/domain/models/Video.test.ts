import { describe, expect, it } from 'vitest';
import { toVideoEmbed } from './Video';

describe('toVideoEmbed', () => {
  it('intègre YouTube par son adresse sans cookie', () => {
    expect(toVideoEmbed('https://www.youtube.com/watch?v=dQw4w9WgXcQ')).toEqual({
      kind: 'iframe',
      src: 'https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ',
      title: 'Vidéo YouTube',
    });
    expect(toVideoEmbed('https://youtu.be/dQw4w9WgXcQ')).toMatchObject({
      src: 'https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ',
    });
    // Une adresse d'intégration déjà formée est reconnue, pas doublée.
    expect(toVideoEmbed('https://www.youtube.com/embed/dQw4w9WgXcQ')).toMatchObject({
      src: 'https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ',
    });
  });

  it('intègre Vimeo par son lecteur', () => {
    expect(toVideoEmbed('https://vimeo.com/123456789')).toEqual({
      kind: 'iframe',
      src: 'https://player.vimeo.com/video/123456789',
      title: 'Vidéo Vimeo',
    });
  });

  it('lit un fichier vidéo directement', () => {
    expect(toVideoEmbed('https://exemple.fr/cours/intro.mp4')).toEqual({
      kind: 'file',
      src: 'https://exemple.fr/cours/intro.mp4',
    });
    expect(toVideoEmbed('https://exemple.fr/intro.webm?t=2')).toMatchObject({ kind: 'file' });
  });

  it("n'intègre jamais une plateforme inconnue : elle reste un lien", () => {
    expect(toVideoEmbed('https://exemple.fr/une/page')).toEqual({
      kind: 'link',
      src: 'https://exemple.fr/une/page',
    });
    // Une adresse qui ressemble à YouTube sans l'être n'est pas intégrée.
    expect(toVideoEmbed('https://youtube.com.pirate.fr/watch?v=abcdefg')).toMatchObject({ kind: 'link' });
  });
});
