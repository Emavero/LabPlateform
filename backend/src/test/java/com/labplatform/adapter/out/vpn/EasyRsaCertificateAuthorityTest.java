package com.labplatform.adapter.out.vpn;

import com.labplatform.adapter.out.process.ProcessCommandRunner;
import com.labplatform.application.port.out.VpnCertificateAuthorityPort.ClientCredentials;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Test d'intégration avec le vrai easy-rsa ; ignoré si l'outil n'est pas installé. */
class EasyRsaCertificateAuthorityTest {

    private static final String EASYRSA = "/usr/share/easy-rsa/easyrsa";

    private EasyRsaCertificateAuthority authority;
    private Path directory;

    @BeforeEach
    void setUp() throws IOException {
        Assumptions.assumeTrue(Files.isExecutable(Path.of(EASYRSA)), "easy-rsa absent : test ignoré");
        directory = Files.createTempDirectory("cyberMans-vpn-test");
        authority = new EasyRsaCertificateAuthority(
                new EasyRsaSettings(EASYRSA, directory, "cyberMans-Test-CA", "serveur",
                        Duration.ofSeconds(60), Duration.ofDays(1)),
                new ProcessCommandRunner(), Clock.systemUTC());
    }

    @Test
    void creeLaPkiEmetEtRevoqueUnCertificat() throws IOException {
        ClientCredentials client = authority.issueClient("cyberMans-u1-abcdef1234");

        assertTrue(client.certificatePem().contains("BEGIN CERTIFICATE"));
        assertTrue(client.privateKeyPem().contains("PRIVATE KEY"));
        assertTrue(authority.caCertificate().contains("BEGIN CERTIFICATE"));
        assertTrue(authority.tlsCryptKey().contains("BEGIN OpenVPN Static key V1"));
        assertTrue(Files.exists(directory.resolve("pki/issued/serveur.crt")), "certificat serveur créé");

        authority.revokeClient("cyberMans-u1-abcdef1234");

        assertFalse(authority.findClient("cyberMans-u1-abcdef1234").isPresent());
        assertTrue(authority.revocationList().contains("BEGIN X509 CRL"));
    }

    @Test
    void revoquerUnCertificatInconnuEstSansEffet() {
        authority.revokeClient("cyberMans-u9-inconnu0000");
    }

    @Test
    void refuseUnNomDeCertificatDangereux() {
        try {
            authority.issueClient("../../etc/passwd");
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("nom dangereux accepté");
    }
}
