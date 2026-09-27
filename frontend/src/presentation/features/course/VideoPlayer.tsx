import { toVideoEmbed } from '@/domain/models/Video';
import { Icon } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';

/**
 * Vidéo d'une section. Seules les plateformes connues sont intégrées à la
 * page ; toute autre adresse est proposée en lien, jamais chargée ici.
 */
export function VideoPlayer({ url, title }: { url: string; title: string }) {
  const { t } = useI18n();
  const embed = toVideoEmbed(url);

  if (embed.kind === 'iframe') {
    return (
      <div className="video">
        <iframe
          className="video__frame"
          src={embed.src}
          title={`${title} — ${embed.title}`}
          loading="lazy"
          referrerPolicy="strict-origin-when-cross-origin"
          allow="accelerometer; clipboard-write; encrypted-media; picture-in-picture; fullscreen"
          allowFullScreen
        />
      </div>
    );
  }

  if (embed.kind === 'file') {
    return (
      <div className="video">
        {/* eslint-disable-next-line jsx-a11y/media-has-caption -- la piste de sous-titres n'est pas fournie par l'auteur */}
        <video className="video__frame" src={embed.src} controls preload="metadata" />
      </div>
    );
  }

  return (
    <p className="video__link">
      <Icon name="play" size={14} />
      <a href={embed.src} target="_blank" rel="noreferrer noopener">
        {t('video.openInNewTab')}
      </a>
    </p>
  );
}
