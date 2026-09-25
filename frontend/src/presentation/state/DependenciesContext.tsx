import { createContext, useContext, type ReactNode } from 'react';
import type { Dependencies } from '@/di/container';

const DependenciesContext = createContext<Dependencies | null>(null);

/** Injecte les cas d'usage dans l'arbre React (inversion de dépendances). */
export function DependenciesProvider({ value, children }: { value: Dependencies; children: ReactNode }) {
  return <DependenciesContext.Provider value={value}>{children}</DependenciesContext.Provider>;
}

export function useDependencies(): Dependencies {
  const dependencies = useContext(DependenciesContext);
  if (!dependencies) throw new Error('useDependencies doit être utilisé sous <DependenciesProvider>.');
  return dependencies;
}
