import { useCallback, useEffect, useRef, useState } from 'react';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import { availableAction, type MachineAction, type TargetMachine } from '@/domain/models/TargetMachine';
import { useLanguageRefresh } from './useLanguageRefresh';
import { useDependencies } from '../state/DependenciesContext';

/** Intervalle entre deux interrogations pendant une transition. */
const POLL_INTERVAL_MS = 4000;

/**
 * Nombre d'interrogations avant d'abandonner.
 * <p>
 * Dix minutes à ce rythme : au-delà, une machine qui n'a pas fini de démarrer
 * ne finira pas, et continuer indéfiniment ferait battre l'onglet d'un
 * utilisateur parti depuis longtemps. L'état reste affiché, seule
 * l'interrogation s'arrête.
 */
const MAX_POLLS = 150;

export interface TargetMachineState {
  machine: TargetMachine | null;
  loading: boolean;
  /** Une demande est en cours d'envoi : différent d'une machine qui transite. */
  pending: MachineAction | null;
  /** L'interrogation périodique tourne : la machine bouge et on la suit. */
  watching: boolean;
  /** L'interrogation s'est arrêtée avant d'atteindre un état stable. */
  gaveUp: boolean;
  error: AppError | null;
  toggle: () => Promise<void>;
  reload: () => Promise<void>;
}

/**
 * Cible partagée : son état, et le bouton qui l'allume ou l'éteint.
 * <p>
 * Pendant une transition, l'état est redemandé toutes les quelques secondes
 * jusqu'à ce qu'il se pose. L'intervalle est relancé à chaque réponse plutôt
 * que posé une fois pour toutes : deux requêtes ne peuvent ainsi pas se
 * chevaucher si le serveur traîne, ce qu'un `setInterval` n'aurait pas évité.
 */
export function useTargetMachine(): TargetMachineState {
  const { machine: api } = useDependencies();
  const [machine, setMachine] = useState<TargetMachine | null>(null);
  const [loading, setLoading] = useState(true);
  const [pending, setPending] = useState<MachineAction | null>(null);
  const [error, setError] = useState<AppError | null>(null);
  const [gaveUp, setGaveUp] = useState(false);

  // Références plutôt qu'état : le minuteur ne doit pas provoquer de rendu, et
  // le compteur ne s'affiche nulle part.
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const polls = useRef(0);
  const alive = useRef(true);

  const cancelPolling = useCallback(() => {
    if (timer.current !== null) {
      clearTimeout(timer.current);
      timer.current = null;
    }
  }, []);

  /** Programme la prochaine interrogation si la machine est encore en route. */
  const scheduleIfTransitioning = useCallback(
    (state: TargetMachine, poll: () => void) => {
      cancelPolling();
      if (!state.transitioning) {
        polls.current = 0;
        return;
      }
      if (polls.current >= MAX_POLLS) {
        setGaveUp(true);
        return;
      }
      polls.current += 1;
      timer.current = setTimeout(poll, POLL_INTERVAL_MS);
    },
    [cancelPolling],
  );

  const poll = useCallback(async () => {
    try {
      const state = await api.status.execute();
      // Le composant a pu être démonté pendant la requête : ne rien écrire alors.
      if (!alive.current) return;
      setMachine(state);
      setError(null);
      scheduleIfTransitioning(state, () => void poll());
    } catch (e) {
      if (!alive.current) return;
      // Une interrogation qui échoue arrête la boucle : insister sur une erreur
      // réseau ne ferait que la répéter. Le bouton « Actualiser » reprend la main.
      cancelPolling();
      setError(toAppError(e));
    }
  }, [api.status, cancelPolling, scheduleIfTransitioning]);

  const reload = useCallback(async () => {
    setLoading(true);
    setGaveUp(false);
    polls.current = 0;
    await poll();
    if (alive.current) setLoading(false);
  }, [poll]);

  useEffect(() => {
    alive.current = true;
    void reload();
    return () => {
      alive.current = false;
      cancelPolling();
    };
  }, [reload, cancelPolling]);

  // Le libellé de l'état vient du serveur : il doit être redemandé à la langue.
  useLanguageRefresh(reload);

  const toggle = useCallback(async () => {
    if (!machine) return;
    const action = availableAction(machine);
    setPending(action);
    setError(null);
    setGaveUp(false);
    polls.current = 0;
    try {
      const state = await api.runAction.execute(action);
      if (!alive.current) return;
      setMachine(state);
      scheduleIfTransitioning(state, () => void poll());
    } catch (e) {
      if (!alive.current) return;
      setError(toAppError(e));
      // Refus du serveur (déjà allumée, déjà en train de bouger) : son état fait
      // foi, on le relit plutôt que de garder le nôtre.
      await poll();
    } finally {
      if (alive.current) setPending(null);
    }
  }, [machine, api.runAction, poll, scheduleIfTransitioning]);

  return {
    machine,
    loading,
    pending,
    // Déduit de l'état, et non du minuteur : une référence lue au rendu ne
    // déclencherait pas de nouvel affichage quand elle change.
    watching: (machine?.transitioning ?? false) && !gaveUp && error === null,
    gaveUp,
    error,
    toggle,
    reload,
  };
}
