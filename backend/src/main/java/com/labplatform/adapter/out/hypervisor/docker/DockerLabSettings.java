package com.labplatform.adapter.out.hypervisor.docker;

import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Réglages des machines Linux en conteneurs Docker.
 *
 * @param publicHost    hôte affiché à l'utilisateur pour se connecter (IP ou nom du serveur Docker)
 * @param bindAddress   interface sur laquelle le port SSH est publié (127.0.0.1 = poste local uniquement)
 * @param network       réseau Docker des conteneurs de lab ; vide = réseau par défaut
 * @param pollInterval  intervalle entre deux vérifications de disponibilité
 */
public record DockerLabSettings(
        String dockerBinary,
        String image,
        String publicHost,
        String bindAddress,
        String network,
        String memory,
        String cpus,
        int pidsLimit,
        String containerPrefix,
        String username,
        Duration commandTimeout,
        Duration readyTimeout,
        Duration pollInterval) {

    private static final Pattern USERNAME = Pattern.compile("[a-z_][a-z0-9_-]{0,31}");
    private static final Pattern CONTAINER_PREFIX = Pattern.compile("[a-zA-Z0-9][a-zA-Z0-9_.-]{0,40}");

    public DockerLabSettings {
        Objects.requireNonNull(dockerBinary, "dockerBinary");
        Objects.requireNonNull(image, "image");
        Objects.requireNonNull(publicHost, "publicHost");
        Objects.requireNonNull(bindAddress, "bindAddress");
        Objects.requireNonNull(commandTimeout, "commandTimeout");
        Objects.requireNonNull(readyTimeout, "readyTimeout");
        Objects.requireNonNull(pollInterval, "pollInterval");
        if (image.isBlank() || publicHost.isBlank()) {
            throw new IllegalArgumentException("L'image et l'hôte public des machines Linux sont obligatoires");
        }
        // Ces valeurs finissent en arguments de commandes : on les restreint à un format sûr.
        if (username == null || !USERNAME.matcher(username).matches()) {
            throw new IllegalArgumentException("Nom d'utilisateur de lab invalide : " + username);
        }
        if (containerPrefix == null || !CONTAINER_PREFIX.matcher(containerPrefix).matches()) {
            throw new IllegalArgumentException("Préfixe de conteneur invalide : " + containerPrefix);
        }
        network = network == null ? "" : network.trim();
    }
}
