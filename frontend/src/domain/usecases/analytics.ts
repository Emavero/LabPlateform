import { WINDOWS, type Analytics } from '../models/Analytics';
import type { AnalyticsRepository } from '../repositories/AnalyticsRepository';

/**
 * Indicateurs d'administration. La fenêtre demandée est ramenée à l'une de
 * celles que l'interface propose : une valeur arbitraire donnerait des chiffres
 * qu'aucun autre écran ne permet de recouper.
 */
export class GetAnalyticsUseCase {
  constructor(private readonly analytics: AnalyticsRepository) {}

  execute(windowDays: number): Promise<Analytics> {
    const window = WINDOWS.includes(windowDays) ? windowDays : 30;
    return this.analytics.get(window);
  }
}
