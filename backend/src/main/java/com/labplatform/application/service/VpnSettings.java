package com.labplatform.application.service;

import com.labplatform.domain.vpn.VpnEndpoint;

import java.util.List;
import java.util.Objects;

/**
 * Réglages du VPN, indépendants du framework.
 *
 * @param endpoints  points d'entrée publics proposés au téléchargement (UDP, TCP ou les deux)
 * @param labNetwork réseau des machines, joignable une fois connecté
 */
public record VpnSettings(boolean enabled, List<VpnEndpoint> endpoints, String labNetwork) {

    public VpnSettings {
        endpoints = List.copyOf(Objects.requireNonNull(endpoints, "endpoints"));
        Objects.requireNonNull(labNetwork, "labNetwork");
        if (enabled && endpoints.isEmpty()) {
            throw new IllegalArgumentException("VPN activé sans point d'entrée : renseignez l'hôte UDP ou TCP");
        }
    }
}
