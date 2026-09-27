import type { LabExposure } from '../models/Exposure';

export interface ExposureRepository {
  /** Surface d'attaque du lab, notée pour le compte connecté. */
  get(): Promise<LabExposure>;
}
