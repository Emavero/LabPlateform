import { Icon } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';

/**
 * Ce que fait l'analyse d'exposition, en trois temps.
 * <p>
 * Le texte seul laissait la question ouverte : les apprenants demandaient à
 * quoi sert ce module. Un schéma répond en une seconde là où un paragraphe
 * demande une lecture — à condition de montrer le mécanisme réel, et non une
 * illustration décorative. Les trois colonnes sont donc exactement les trois
 * étapes du calcul : ce qu'on sait, ce qu'on en pèse, ce qui en sort.
 * <p>
 * En HTML plutôt qu'en SVG : les libellés sont traduits, et leur longueur
 * change d'une langue à l'autre. Du texte dans un SVG déborderait de sa boîte,
 * là où une boîte HTML s'adapte toute seule. Les couleurs viennent des jetons,
 * donc le schéma suit le thème clair comme le sombre.
 */
export function ExposureDiagram() {
  const { t } = useI18n();

  return (
    <figure className="schema">
      <div className="schema__flow">
        <div className="schema__step">
          <p className="schema__step-title">
            <Icon name="book" size={15} /> {t('exposure.schemaInputs')}
          </p>
          <ul className="schema__list">
            <li>{t('exposure.schemaInput1')}</li>
            <li>{t('exposure.schemaInput2')}</li>
            <li>{t('exposure.schemaInput3')}</li>
            <li>{t('exposure.schemaInput4')}</li>
          </ul>
        </div>

        <span className="schema__arrow" aria-hidden="true" />

        <div className="schema__step schema__step--accent">
          <p className="schema__step-title">
            <Icon name="radar" size={15} /> {t('exposure.schemaWeights')}
          </p>
          {/* Les poids affichés sont ceux du domaine, pas des exemples : un
              schéma qui invente ses chiffres apprend une fausse mécanique. */}
          <ul className="schema__list schema__list--weights">
            <li>
              {t('exposure.schemaWeightOpen')} <b className="schema__plus">+22</b>
            </li>
            <li>
              {t('exposure.schemaWeightOwned')} <b className="schema__plus">+20</b>
            </li>
            <li>
              {t('exposure.schemaWeightEasy')} <b className="schema__plus">+18</b>
            </li>
            <li>
              {t('exposure.schemaWeightRetired')} <b className="schema__minus">−15</b>
            </li>
          </ul>
        </div>

        <span className="schema__arrow" aria-hidden="true" />

        <div className="schema__step">
          <p className="schema__step-title">
            <Icon name="alert" size={15} /> {t('exposure.schemaOutput')}
          </p>
          <ul className="schema__levels">
            <li className="schema__level schema__level--critical">{t('exposure.level.CRITICAL')}</li>
            <li className="schema__level schema__level--high">{t('exposure.level.HIGH')}</li>
            <li className="schema__level schema__level--moderate">{t('exposure.level.MODERATE')}</li>
            <li className="schema__level schema__level--low">{t('exposure.level.LOW')}</li>
          </ul>
        </div>
      </div>

      <figcaption className="schema__caption">
        <Icon name="info" size={14} /> {t('exposure.schemaCaption')}
      </figcaption>
    </figure>
  );
}
