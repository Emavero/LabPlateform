package com.labplatform.application.service;

/**
 * D'où vient le profil que l'apprenant télécharge.
 * <p>
 * Deux façons de distribuer l'accès, et elles ne se valent pas. La plateforme
 * peut émettre un certificat par personne — révocable individuellement, mais
 * demandant une autorité de certification à tenir. Ou l'administrateur dépose
 * un fichier produit par son propre serveur — rien à tenir, mais le même accès
 * pour tous : retirer celui d'une seule personne devient impossible, on ne peut
 * que remplacer le fichier pour l'ensemble.
 * <p>
 * Le choix revient à l'exploitant ; docs/VPN.md expose le compromis.
 */
public enum VpnSource {

    /** La plateforme émet et révoque les certificats (easy-rsa). */
    GENERATED,
    /** L'administrateur dépose un profil, partagé par tous. */
    UPLOADED
}
