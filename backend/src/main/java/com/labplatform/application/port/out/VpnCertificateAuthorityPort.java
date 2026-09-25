package com.labplatform.application.port.out;

import java.util.Optional;

/**
 * Autorité de certification du VPN : émet et révoque les certificats clients,
 * fournit les éléments communs (certificat de l'AC, clé tls-crypt, liste de révocation).
 */
public interface VpnCertificateAuthorityPort {

    ClientCredentials issueClient(String commonName);

    /** Sans effet si le certificat n'existe pas ou est déjà révoqué. */
    void revokeClient(String commonName);

    Optional<ClientCredentials> findClient(String commonName);

    String caCertificate();

    String tlsCryptKey();

    String revocationList();

    record ClientCredentials(String certificatePem, String privateKeyPem) {

        @Override
        public String toString() {
            // La clé privée ne doit jamais apparaître dans les journaux.
            return "ClientCredentials[certificat présent, clé masquée]";
        }
    }
}
