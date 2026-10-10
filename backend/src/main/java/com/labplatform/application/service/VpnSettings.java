package com.labplatform.application.service;

import com.labplatform.domain.vpn.VpnEndpoint;

import java.util.List;
import java.util.Objects;

/**
 * Réglages du VPN, indépendants du framework.
 *
 * @param endpoints  points d'entrée publics proposés au téléchargement (UDP, TCP ou les deux)
 * @param labNetwork réseau des machines, joignable une fois connecté
 * @param source     d'où vient le profil remis à l'apprenant
 */
public record VpnSettings(boolean enabled, List<VpnEndpoint> endpoints, String labNetwork, VpnSource source) {

    /** Par défaut : profils émis par la plateforme, comme auparavant. */
    public VpnSettings(boolean enabled, List<VpnEndpoint> endpoints, String labNetwork) {
        this(enabled, endpoints, labNetwork, VpnSource.GENERATED);
    }

    public VpnSettings {
        endpoints = List.copyOf(Objects.requireNonNull(endpoints, "endpoints"));
        Objects.requireNonNull(labNetwork, "labNetwork");
        source = source == null ? VpnSource.GENERATED : source;
        // Un profil déposé porte déjà son serveur : il n'y a pas de point
        // d'entrée à choisir, et en exiger un empêcherait de démarrer.
        if (enabled && source == VpnSource.GENERATED && endpoints.isEmpty()) {
            throw new IllegalArgumentException("VPN activé sans point d'entrée : renseignez l'hôte UDP ou TCP");
        }
    }

    public boolean isUploaded() {
        return source == VpnSource.UPLOADED;
    }
}
