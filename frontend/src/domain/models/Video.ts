/**
 * Lecture d'une vidéo de cours.
 *
 * Seules les plateformes connues sont intégrées dans la page, et seulement
 * par leur adresse d'intégration : le reste s'ouvre dans un nouvel onglet.
 * C'est la même liste que celle autorisée par la politique de sécurité de
 * contenu servie par Nginx — élargir l'une sans l'autre ne sert à rien.
 */
export type VideoEmbed =
  | { readonly kind: 'iframe'; readonly src: string; readonly title: string }
  | { readonly kind: 'file'; readonly src: string }
  | { readonly kind: 'link'; readonly src: string };

const YOUTUBE_WATCH = /^https?:\/\/(?:www\.)?youtube\.com\/watch\?(?:.*&)?v=([\w-]{6,20})/i;
const YOUTUBE_SHORT = /^https?:\/\/youtu\.be\/([\w-]{6,20})/i;
const YOUTUBE_EMBED = /^https?:\/\/(?:www\.)?youtube(?:-nocookie)?\.com\/embed\/([\w-]{6,20})/i;
const VIMEO = /^https?:\/\/(?:www\.)?vimeo\.com\/(\d{6,12})/i;
const VIMEO_PLAYER = /^https?:\/\/player\.vimeo\.com\/video\/(\d{6,12})/i;
const FILE = /\.(mp4|webm|ogg|ogv)(\?.*)?$/i;
/** Vidéo téléversée sur la plateforme : servie par notre propre API. */
const HOSTED = /^\/api\/media\/[0-9a-f]{32}$/;

export function toVideoEmbed(url: string): VideoEmbed {
  if (HOSTED.test(url)) {
    return { kind: 'file', src: url };
  }
  const youtubeId =
    url.match(YOUTUBE_WATCH)?.[1] ?? url.match(YOUTUBE_SHORT)?.[1] ?? url.match(YOUTUBE_EMBED)?.[1];
  if (youtubeId) {
    return { kind: 'iframe', src: `https://www.youtube-nocookie.com/embed/${youtubeId}`, title: 'Vidéo YouTube' };
  }
  const vimeoId = url.match(VIMEO)?.[1] ?? url.match(VIMEO_PLAYER)?.[1];
  if (vimeoId) {
    return { kind: 'iframe', src: `https://player.vimeo.com/video/${vimeoId}`, title: 'Vidéo Vimeo' };
  }
  if (FILE.test(url)) {
    return { kind: 'file', src: url };
  }
  return { kind: 'link', src: url };
}
