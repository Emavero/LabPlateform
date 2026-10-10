package com.labplatform.domain.vpn;

import com.labplatform.domain.shared.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le profil déposé par l'administration. La validation se fait au dépôt, quand
 * l'administrateur peut encore corriger — pas des semaines plus tard, quand un
 * apprenant n'arrive pas à se connecter.
 */
class LabVpnProfileTest {

    private static final Instant NOW = Instant.parse("2026-10-10T09:00:00Z");

    private static final String VALID = """
            client
            dev tun
            proto udp
            remote vpn.exemple.test 1194
            route 10.10.10.0 255.255.255.0
            <ca>
            -----BEGIN CERTIFICATE-----
            -----END CERTIFICATE-----
            </ca>
            """;

    @Test
    void acceptsAClientProfile() {
        LabVpnProfile profile = LabVpnProfile.of("cyberMans-lab.ovpn", VALID, NOW);

        assertEquals("cyberMans-lab.ovpn", profile.fileName());
        assertEquals(NOW, profile.uploadedAt());
        assertTrue(profile.sizeBytes() > 0);
    }

    @Test
    void refusesWhatIsNotAClientProfile() {
        // Configuration de serveur déposée à la place de celle d'un client :
        // l'erreur la plus facile à commettre, et la plus pénible à diagnostiquer.
        String server = "port 1194\nproto udp\ndev tun\nserver 10.8.0.0 255.255.255.0\n";

        assertEquals("Ce fichier n'est pas un profil client OpenVPN : il lui manque « client » ou « remote »",
                assertThrows(InvalidInputException.class,
                        () -> LabVpnProfile.of("serveur.ovpn", server, NOW)).getMessage());
    }

    @Test
    void refusesAProfileWithoutRemote() {
        assertThrows(InvalidInputException.class,
                () -> LabVpnProfile.of("sans-remote.ovpn", "client\ndev tun\n", NOW));
    }

    @Test
    void refusesAnEmptyOrBinaryFile() {
        assertThrows(InvalidInputException.class, () -> LabVpnProfile.of("vide.ovpn", "   ", NOW));
        assertThrows(InvalidInputException.class,
                () -> LabVpnProfile.of("image.ovpn", "client\nremote a 1\n\0\u0001", NOW));
    }

    @Test
    void refusesAFileTooLargeToBeAProfile() {
        String huge = VALID + "#".repeat(LabVpnProfile.MAX_SIZE_BYTES);

        assertThrows(InvalidInputException.class, () -> LabVpnProfile.of("gros.ovpn", huge, NOW));
    }

    @Test
    void refusesANameThatIsNotAnOvpnFile() {
        assertThrows(InvalidInputException.class, () -> LabVpnProfile.of("profil.txt", VALID, NOW));
        assertThrows(InvalidInputException.class, () -> LabVpnProfile.of("", VALID, NOW));
        assertThrows(InvalidInputException.class, () -> LabVpnProfile.of(null, VALID, NOW));
        // Remontée de chemin : le nom revient à l'apprenant, il ne sert jamais
        // de nom de fichier sur le disque, mais autant le refuser ici aussi.
        assertThrows(InvalidInputException.class, () -> LabVpnProfile.of("../../etc/passwd.ovpn", VALID, NOW));
    }

    @Test
    void seesWhetherTheProfileRoutesTheLabNetwork() {
        LabNetwork lab = LabNetwork.ofCidr("10.10.10.0/24");

        assertTrue(LabVpnProfile.of("bon.ovpn", VALID, NOW).routes(lab));
        // Sans route, le tunnel monte et l'adresse de la cible ne répond pas.
        String noRoute = "client\ndev tun\nremote vpn.exemple.test 1194\n";
        assertFalse(LabVpnProfile.of("sans-route.ovpn", noRoute, NOW).routes(lab));
    }

    @Test
    void acceptsAProfileThatRoutesEverything() {
        // redirect-gateway fait passer tout le trafic par le tunnel : le réseau
        // du lab en fait partie, il n'y a pas d'avertissement à donner.
        String all = "client\ndev tun\nremote vpn.exemple.test 1194\nredirect-gateway def1\n";

        assertTrue(LabVpnProfile.of("tout.ovpn", all, NOW).routes(LabNetwork.ofCidr("10.10.10.0/24")));
    }
}
