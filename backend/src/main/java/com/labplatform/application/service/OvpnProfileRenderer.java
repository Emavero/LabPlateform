package com.labplatform.application.service;

import com.labplatform.application.port.out.VpnCertificateAuthorityPort.ClientCredentials;
import com.labplatform.domain.vpn.LabNetwork;
import com.labplatform.domain.vpn.VpnEndpoint;
import com.labplatform.domain.vpn.VpnProtocol;

/**
 * Assemble un profil OpenVPN autonome : réglages client, route vers le lab et
 * certificats intégrés, pour qu'un seul fichier suffise à se connecter et à
 * joindre les machines.
 * <p>
 * La route est écrite dans le profil plutôt que laissée au serveur : sans elle,
 * le tunnel monte et l'adresse interne d'une cible — celle qu'affiche le bouton
 * « Démarrer » — reste injoignable, panne silencieuse qui ressemble à une
 * machine en panne. Un serveur qui pousse déjà la même route ne crée pas de
 * conflit : OpenVPN installe la même entrée deux fois sans dommage.
 */
public final class OvpnProfileRenderer {

    private static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    private static final String END_CERT = "-----END CERTIFICATE-----";

    public String render(String commonName, VpnEndpoint endpoint, LabNetwork labNetwork, String caCertificate,
                         ClientCredentials credentials, String tlsCryptKey) {
        StringBuilder out = new StringBuilder();
        line(out, "# cyberMans Lab — profil VPN personnel (" + commonName + ")");
        line(out, "# Ne partagez pas ce fichier : il donne accès au lab en votre nom.");
        line(out, "client");
        line(out, "dev tun");
        line(out, endpoint.protocol() == VpnProtocol.UDP ? "proto udp" : "proto tcp-client");
        line(out, "remote " + endpoint.host() + " " + endpoint.port());
        line(out, "resolv-retry infinite");
        line(out, "nobind");
        line(out, "persist-key");
        line(out, "persist-tun");
        line(out, "remote-cert-tls server");
        line(out, "data-ciphers AES-256-GCM:AES-128-GCM:CHACHA20-POLY1305");
        if (endpoint.protocol() == VpnProtocol.UDP) {
            // Prévient le serveur à la déconnexion : l'adresse VPN est libérée tout de suite.
            line(out, "explicit-exit-notify 1");
        }
        // Seul le réseau du lab passe par le tunnel : le reste du trafic de
        // l'apprenant continue de sortir par sa connexion habituelle.
        line(out, "route " + labNetwork.address() + " " + labNetwork.netmask());
        line(out, "verb 3");
        block(out, "ca", caCertificate.strip());
        block(out, "cert", certificateOnly(credentials.certificatePem()));
        block(out, "key", credentials.privateKeyPem().strip());
        block(out, "tls-crypt", tlsCryptKey.strip());
        return out.toString();
    }

    /** easy-rsa préfixe le certificat d'une description texte : on ne garde que le bloc PEM. */
    static String certificateOnly(String pem) {
        int start = pem.indexOf(BEGIN_CERT);
        int end = pem.indexOf(END_CERT);
        if (start < 0 || end < start) {
            throw new IllegalStateException("Certificat client illisible");
        }
        return pem.substring(start, end + END_CERT.length());
    }

    private static void block(StringBuilder out, String tag, String content) {
        line(out, "<" + tag + ">");
        line(out, content);
        line(out, "</" + tag + ">");
    }

    private static void line(StringBuilder out, String text) {
        out.append(text).append('\n');
    }
}
