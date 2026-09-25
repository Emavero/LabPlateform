import { useEffect, useRef, useState } from 'react';
import { Icon } from './Icon';

interface CopyFieldProps {
  label: string;
  value: string;
  /** Valeur sensible : masquée par défaut, révélable, toujours copiable. */
  secret?: boolean;
}

async function copyToClipboard(text: string): Promise<boolean> {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
      return true;
    }
  } catch {
    // on tente la méthode de repli ci-dessous
  }
  // Repli pour les contextes non sécurisés (http://<ip>) où l'API Clipboard est absente.
  const area = document.createElement('textarea');
  area.value = text;
  area.setAttribute('readonly', '');
  area.style.position = 'fixed';
  area.style.opacity = '0';
  document.body.appendChild(area);
  area.select();
  const ok = document.execCommand('copy');
  document.body.removeChild(area);
  return ok;
}

export function CopyField({ label, value, secret = false }: CopyFieldProps) {
  const [revealed, setRevealed] = useState(!secret);
  const [copied, setCopied] = useState(false);
  const timer = useRef<number | undefined>(undefined);

  useEffect(() => () => window.clearTimeout(timer.current), []);

  const copy = async () => {
    if (await copyToClipboard(value)) {
      setCopied(true);
      window.clearTimeout(timer.current);
      timer.current = window.setTimeout(() => setCopied(false), 1600);
    }
  };

  return (
    <div className="copy-field">
      <span className="copy-field__label">{label}</span>
      <div className="copy-field__row">
        <code className="copy-field__value">{revealed ? value : '•'.repeat(Math.min(value.length, 14))}</code>
        {secret && (
          <button
            type="button"
            className="icon-btn"
            onClick={() => setRevealed((v) => !v)}
            aria-label={revealed ? `Masquer ${label.toLowerCase()}` : `Afficher ${label.toLowerCase()}`}
            aria-pressed={revealed}
          >
            <Icon name={revealed ? 'eyeOff' : 'eye'} size={16} />
          </button>
        )}
        <button type="button" className="icon-btn" onClick={copy} aria-label={`Copier ${label.toLowerCase()}`}>
          <Icon name={copied ? 'check' : 'copy'} size={16} />
        </button>
        <span className="visually-hidden" aria-live="polite">
          {copied ? `${label} copié` : ''}
        </span>
      </div>
    </div>
  );
}
