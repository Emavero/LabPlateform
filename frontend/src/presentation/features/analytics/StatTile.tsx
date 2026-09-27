import { compact, deltaPercent } from '@/domain/models/Analytics';
import { Icon } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';

interface StatTileProps {
  label: string;
  /** `null` quand il n'y a rien à mesurer : la tuile affiche un tiret, pas un zéro. */
  value: number | null;
  /** Même mesure sur la période précédente : sans elle, aucune variation n'est affichée. */
  previous?: number;
  /** Suffixe collé à la valeur (« % », « j »). */
  unit?: string;
  hint?: string;
  /** Faux quand une hausse est une mauvaise nouvelle (comptes dormants, échecs). */
  upIsGood?: boolean;
}

/**
 * Un chiffre et sa variation.
 * <p>
 * Ce n'est pas un graphique à une barre : une valeur isolée se lit mieux en
 * chiffres. La variation est comparée à la période précédente de même longueur,
 * et la couleur suit le <em>sens</em> de la nouvelle, pas celui de la flèche —
 * plus de comptes dormants est une mauvaise nouvelle même si le nombre monte.
 * <p>
 * Une mesure sans données vaut {@code null} et s'affiche en tiret : un taux à
 * 0 % se lirait comme un résultat, alors qu'il n'y a rien eu à mesurer.
 */
export function StatTile({ label, value, previous, unit, hint, upIsGood = true }: StatTileProps) {
  const { t } = useI18n();
  const delta = previous === undefined || value === null ? null : deltaPercent(value, previous);
  const good = delta === null || delta === 0 ? null : (delta > 0) === upIsGood;

  return (
    <article className="stat-tile">
      <p className="stat-tile__label">{label}</p>
      <p className={['stat-tile__value', value === null && 'stat-tile__value--none'].filter(Boolean).join(' ')}>
        {value === null ? '—' : compact(value)}
        {value !== null && unit && <span className="stat-tile__unit">{unit}</span>}
      </p>
      {value !== null && previous !== undefined && (
        <p
          className={['stat-tile__delta', good === null ? '' : good ? 'stat-tile__delta--good' : 'stat-tile__delta--bad']
            .filter(Boolean)
            .join(' ')}
        >
          {delta === null ? (
            t('stat.new')
          ) : (
            <>
              <Icon name={delta >= 0 ? 'chevronUp' : 'chevronDown'} size={13} />
              {t('stat.vsPrevious', { percent: Math.abs(delta) })}
            </>
          )}
        </p>
      )}
      {hint && <p className="stat-tile__hint">{hint}</p>}
    </article>
  );
}
