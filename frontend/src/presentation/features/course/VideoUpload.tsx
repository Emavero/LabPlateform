import { useId, useState, type ChangeEvent } from 'react';
import { MEDIA_TYPES } from '@/domain/models/Admin';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import { Icon, Spinner } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';
import { useDependencies } from '../../state/DependenciesContext';

interface VideoUploadProps {
  /** Reçoit l'adresse de la vidéo une fois le fichier déposé sur la plateforme. */
  onUploaded: (url: string) => void;
}

/**
 * Téléversement d'une vidéo hébergée par la plateforme, à côté de la saisie
 * d'une adresse externe : l'un remplit l'autre.
 */
export function VideoUpload({ onUploaded }: VideoUploadProps) {
  const { t, tm } = useI18n();
  const { admin } = useDependencies();
  const inputId = useId();
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<AppError | null>(null);
  const [uploaded, setUploaded] = useState<string | null>(null);

  async function choose(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    // Le champ est remis à zéro : re-choisir le même fichier redéclenche l'envoi.
    event.target.value = '';
    if (!file) return;

    setUploading(true);
    setError(null);
    setUploaded(null);
    try {
      const media = await admin.uploadMedia.execute(file);
      setUploaded(media.filename);
      onUploaded(media.url);
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setUploading(false);
    }
  }

  return (
    <div className="upload">
      <input
        id={inputId}
        className="upload__input"
        type="file"
        accept={MEDIA_TYPES.join(',')}
        onChange={(event) => void choose(event)}
        disabled={uploading}
      />
      <label className="btn btn--ghost btn--sm upload__button" htmlFor={inputId}>
        {uploading ? <Spinner /> : <Icon name="download" size={16} />}
        <span>{t(uploading ? 'upload.uploading' : 'upload.action')}</span>
      </label>
      {uploaded && (
        <span className="upload__done">
          <Icon name="check" size={14} /> {uploaded}
        </span>
      )}
      {error && <span className="field__error">{tm(error.fieldErrors.file ?? error.message)}</span>}
    </div>
  );
}
