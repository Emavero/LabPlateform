#!/bin/sh
# Prépare le compte de lab puis lance le serveur SSH au premier plan.
set -eu

USER_NAME="${LAB_USERNAME:-labuser}"
case "$USER_NAME" in
  ''|-*|*[!a-z0-9_-]*)
    echo "[lab] Nom d'utilisateur invalide : $USER_NAME" >&2
    exit 1
    ;;
esac

if ! id -u "$USER_NAME" >/dev/null 2>&1; then
  # Compte créé verrouillé : aucune connexion possible avant que le backend
  # ait injecté le mot de passe temporaire.
  useradd --create-home --shell /bin/bash --groups sudo "$USER_NAME"
fi

ssh-keygen -A >/dev/null 2>&1
echo "[lab] Compte $USER_NAME créé, serveur SSH en cours de démarrage"
touch /run/lab-ready
exec /usr/sbin/sshd -D -e
