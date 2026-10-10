package com.labplatform.application.port.in.vpn;

import com.labplatform.domain.user.Actor;

import java.util.Optional;

/**
 * Dépôt, consultation et retrait du profil VPN que l'administration fournit
 * aux apprenants.
 */
public interface ManageLabVpnProfileUseCase {

    /** Dépose un profil, en remplacement du précédent s'il y en avait un. */
    LabVpnProfileSummary upload(Actor actor, String fileName, String content);

    /** Ce qui est déposé, vide si rien ne l'est. */
    Optional<LabVpnProfileSummary> current(Actor actor);

    void remove(Actor actor);
}
