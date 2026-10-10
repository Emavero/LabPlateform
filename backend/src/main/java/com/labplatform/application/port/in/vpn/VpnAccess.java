package com.labplatform.application.port.in.vpn;

import com.labplatform.domain.vpn.VpnEndpoint;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * État de l'accès VPN d'un utilisateur, tel qu'affiché sur la page « VPN Access ».
 *
 * @param issuedAt   date d'émission du profil actuel ; vide s'il n'a jamais été téléchargé.
 *                   En source déposée, c'est la date du dépôt par l'administration
 * @param labNetwork réseau joignable une fois connecté (ex. 10.10.10.0/24)
 * @param source     « generated » ou « uploaded » : la page s'affiche
 *                   différemment selon que le profil est émis pour cette
 *                   personne ou déposé pour tous
 */
public record VpnAccess(boolean enabled, Optional<Instant> issuedAt, List<VpnEndpoint> endpoints, String labNetwork,
                        String source) {

    /** Source par défaut : la plateforme émet les profils, comme auparavant. */
    public VpnAccess(boolean enabled, Optional<Instant> issuedAt, List<VpnEndpoint> endpoints, String labNetwork) {
        this(enabled, issuedAt, endpoints, labNetwork, "generated");
    }
}
