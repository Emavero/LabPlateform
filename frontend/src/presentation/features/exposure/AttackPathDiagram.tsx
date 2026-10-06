import { Icon } from '../../design-system';
import { useI18n } from '../../i18n/I18nContext';

/**
 * Ce que fait le module des chemins d'attaque, en un exemple chiffré.
 * <p>
 * Toute l'idée tient dans une comparaison : une cible atteinte depuis une autre
 * coûte moins qu'attaquée de front. Dit en prose, cela reste abstrait ; posé en
 * trois additions, cela se vérifie du regard.
 * <p>
 * Les deux cibles sont fictives, mais les nombres sont ceux que le moteur
 * calculerait : remise de 45 % depuis le même segment, de 25 % depuis un même
 * système, division entière comprise (80 × 55 / 100 = 44). Un schéma qui
 * inventerait ses coefficients enseignerait une mécanique qui n'existe pas.
 * <p>
 * Un seul saut, et non une longue chaîne : chaque étape ajoute son coût remisé
 * au précédent, si bien qu'un détour de trois cibles dépasse presque toujours
 * l'assaut direct. Montrer une chaîne qui gagne demanderait des nombres
 * invraisemblables.
 */
export function AttackPathDiagram() {
  const { t } = useI18n();

  return (
    <figure className="schema">
      <ol className="chain">
        <li className="chain__node">
          <span className="chain__name">{t('paths.schemaDoor')}</span>
          <span className="chain__effort">{t('paths.effort', { effort: 10 })}</span>
        </li>
        <li className="chain__link" aria-hidden="true">
          <span className="chain__reason">{t('paths.schemaSameSegment')}</span>
          <span className="chain__discount">−45 %</span>
        </li>
        <li className="chain__node chain__node--goal">
          <span className="chain__name">{t('paths.schemaGoal')}</span>
          <span className="chain__effort">{t('paths.effort', { effort: 80 })}</span>
        </li>
      </ol>

      {/* La comparaison est le propos : sans elle, la chaîne n'est qu'un dessin. */}
      <table className="chain__compare">
        <caption className="visually-hidden">{t('paths.schemaCompare')}</caption>
        <tbody>
          <tr>
            <th scope="row">{t('paths.schemaHeadOn')}</th>
            <td className="chain__sum" />
            <td className="chain__total">80</td>
          </tr>
          <tr className="chain__row--best">
            <th scope="row">{t('paths.schemaViaSegment')}</th>
            <td className="chain__sum">10 + 44</td>
            <td className="chain__total">54</td>
          </tr>
          <tr className="chain__row--best">
            <th scope="row">{t('paths.schemaViaSystem')}</th>
            <td className="chain__sum">10 + 60</td>
            <td className="chain__total">70</td>
          </tr>
        </tbody>
      </table>

      <figcaption className="schema__caption">
        <Icon name="info" size={14} /> {t('paths.schemaCaption')}
      </figcaption>
    </figure>
  );
}
