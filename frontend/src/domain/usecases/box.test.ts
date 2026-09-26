import { describe, expect, it } from 'vitest';
import type { AppError } from '../errors/AppError';
import type { Box, Difficulty, FlagKind } from '../models/Box';
import type { BoxRepository, FlagSubmission } from '../repositories/BoxRepository';
import { SubmitFlagUseCase } from './box';

const FLAG = '0123456789abcdef0123456789abcdef';

function repository(calls: string[]): BoxRepository {
  return {
    list: async () => [] as Box[],
    get: async () => ({}) as Box,
    submitFlag: async (slug: string, kind: FlagKind, flag: string) => {
      calls.push(`${slug}:${kind}:${flag}`);
      return {} as FlagSubmission;
    },
    rate: async (slug: string, difficulty: Difficulty) => {
      calls.push(`rate:${slug}:${difficulty}`);
      return {} as Box;
    },
  };
}

describe('SubmitFlagUseCase', () => {
  it("normalise le flag avant de l'envoyer au serveur", async () => {
    const calls: string[] = [];
    await new SubmitFlagUseCase(repository(calls)).execute('mirage', 'ROOT', ` CYBM{${FLAG.toUpperCase()}} `);

    expect(calls).toEqual([`mirage:ROOT:${FLAG}`]);
  });

  it("refuse une saisie invalide sans appeler le serveur", async () => {
    const calls: string[] = [];
    const useCase = new SubmitFlagUseCase(repository(calls));

    const error = await useCase
      .execute('mirage', 'USER', 'pas-un-flag')
      .then(() => undefined, (e: AppError) => e);

    expect(error?.kind).toBe('validation');
    expect(error?.fieldErrors.flag).toContain('32 caractères');
    expect(calls).toEqual([]);
  });
});
