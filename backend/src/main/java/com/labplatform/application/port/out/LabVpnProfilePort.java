package com.labplatform.application.port.out;

import com.labplatform.domain.vpn.LabVpnProfile;

import java.util.Optional;

/**
 * Rangement du profil VPN déposé par l'administration.
 * <p>
 * Un seul profil pour toute la plateforme : le déposer remplace le précédent.
 * Le contenu ne passe pas par la base — c'est un fichier, et il se sert comme
 * tel — mais il reste sur le serveur, jamais dans un dossier public.
 */
public interface LabVpnProfilePort {

    void save(LabVpnProfile profile);

    Optional<LabVpnProfile> find();

    void delete();
}
