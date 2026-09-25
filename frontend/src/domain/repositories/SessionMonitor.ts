/** Signale que la session a expiré côté serveur (réponse 401 sur une route protégée). */
export interface SessionMonitor {
  onSessionExpired(listener: () => void): () => void;
}
