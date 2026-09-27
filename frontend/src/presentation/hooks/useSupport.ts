import { useCallback, useEffect, useState } from 'react';
import { toAppError, type AppError } from '@/domain/errors/AppError';
import type { SupportQueue, Ticket, TicketDraft, TicketSummary } from '@/domain/models/Support';
import { useDependencies } from '../state/DependenciesContext';

export interface MyTicketsState {
  tickets: TicketSummary[];
  /** Demande ouverte dans le panneau de lecture, avec son fil. */
  opened: Ticket | null;
  loading: boolean;
  busy: boolean;
  error: AppError | null;
  select: (id: number | null) => Promise<void>;
  open: (draft: TicketDraft) => Promise<boolean>;
  reply: (body: string) => Promise<boolean>;
  resolve: () => Promise<void>;
  reload: () => Promise<void>;
}

/**
 * Demandes du compte connecté.
 * <p>
 * Chaque écriture rafraîchit la liste : le statut d'une demande se déduit de
 * son dernier message, donc répondre change sa place dans la liste. Recharger
 * évite d'avoir à rejouer cette règle dans le navigateur, où elle finirait par
 * s'écarter de celle du serveur.
 */
export function useMyTickets(): MyTicketsState {
  const { support } = useDependencies();
  const [tickets, setTickets] = useState<TicketSummary[]>([]);
  const [opened, setOpened] = useState<Ticket | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<AppError | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setTickets(await support.listMine.execute());
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [support.listMine]);

  useEffect(() => {
    void reload();
  }, [reload]);

  const select = useCallback(
    async (id: number | null) => {
      if (id === null) {
        setOpened(null);
        return;
      }
      setBusy(true);
      setError(null);
      try {
        setOpened(await support.get.execute(id));
      } catch (e) {
        setError(toAppError(e));
      } finally {
        setBusy(false);
      }
    },
    [support.get],
  );

  const open = useCallback(
    async (draft: TicketDraft) => {
      setBusy(true);
      setError(null);
      try {
        setOpened(await support.open.execute(draft));
        await reload();
        return true;
      } catch (e) {
        setError(toAppError(e));
        return false;
      } finally {
        setBusy(false);
      }
    },
    [support.open, reload],
  );

  const reply = useCallback(
    async (body: string) => {
      if (!opened) return false;
      setBusy(true);
      setError(null);
      try {
        setOpened(await support.reply.execute(opened.id, body));
        await reload();
        return true;
      } catch (e) {
        setError(toAppError(e));
        return false;
      } finally {
        setBusy(false);
      }
    },
    [support.reply, opened, reload],
  );

  const resolve = useCallback(async () => {
    if (!opened) return;
    setBusy(true);
    setError(null);
    try {
      setOpened(await support.resolve.execute(opened.id));
      await reload();
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setBusy(false);
    }
  }, [support.resolve, opened, reload]);

  return { tickets, opened, loading, busy, error, select, open, reply, resolve, reload };
}

export interface SupportQueueState {
  queue: SupportQueue | null;
  opened: Ticket | null;
  loading: boolean;
  busy: boolean;
  error: AppError | null;
  select: (id: number | null) => Promise<void>;
  reply: (body: string) => Promise<boolean>;
  resolve: () => Promise<void>;
  reload: () => Promise<void>;
}

/** File d'attente de l'équipe. Le serveur la refuse à qui n'est pas administrateur. */
export function useSupportQueue(): SupportQueueState {
  const { support } = useDependencies();
  const [queue, setQueue] = useState<SupportQueue | null>(null);
  const [opened, setOpened] = useState<Ticket | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<AppError | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      setQueue(await support.queue.execute());
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setLoading(false);
    }
  }, [support.queue]);

  useEffect(() => {
    void reload();
  }, [reload]);

  const select = useCallback(
    async (id: number | null) => {
      if (id === null) {
        setOpened(null);
        return;
      }
      setBusy(true);
      setError(null);
      try {
        setOpened(await support.get.execute(id));
      } catch (e) {
        setError(toAppError(e));
      } finally {
        setBusy(false);
      }
    },
    [support.get],
  );

  const reply = useCallback(
    async (body: string) => {
      if (!opened) return false;
      setBusy(true);
      setError(null);
      try {
        setOpened(await support.reply.execute(opened.id, body));
        await reload();
        return true;
      } catch (e) {
        setError(toAppError(e));
        return false;
      } finally {
        setBusy(false);
      }
    },
    [support.reply, opened, reload],
  );

  const resolve = useCallback(async () => {
    if (!opened) return;
    setBusy(true);
    setError(null);
    try {
      setOpened(await support.resolve.execute(opened.id));
      await reload();
    } catch (e) {
      setError(toAppError(e));
    } finally {
      setBusy(false);
    }
  }, [support.resolve, opened, reload]);

  return { queue, opened, loading, busy, error, select, reply, resolve, reload };
}
