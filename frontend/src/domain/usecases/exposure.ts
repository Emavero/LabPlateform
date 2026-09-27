import type { LabExposure } from '../models/Exposure';
import type { ExposureRepository } from '../repositories/ExposureRepository';

export class GetLabExposureUseCase {
  constructor(private readonly exposure: ExposureRepository) {}

  execute(): Promise<LabExposure> {
    return this.exposure.get();
  }
}
