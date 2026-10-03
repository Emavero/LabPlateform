package com.labplatform.domain.vpn;

import com.labplatform.domain.shared.InvalidInputException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * La route du profil VPN. Une erreur de masque donne un tunnel qui monte sans
 * rien joindre : c'est exactement le genre de panne silencieuse qu'un test
 * attrape et qu'un essai manuel laisse passer.
 */
class LabNetworkTest {

    @Test
    void convertsTheUsualPrefixes() {
        assertEquals("255.255.255.0", LabNetwork.ofCidr("10.10.10.0/24").netmask());
        assertEquals("255.255.0.0", LabNetwork.ofCidr("10.10.0.0/16").netmask());
        assertEquals("255.0.0.0", LabNetwork.ofCidr("10.0.0.0/8").netmask());
        assertEquals("255.255.255.252", LabNetwork.ofCidr("192.168.1.0/30").netmask());
        assertEquals("255.255.255.255", LabNetwork.ofCidr("10.10.10.7/32").netmask());
    }

    @Test
    void keepsTheNetworkAddress() {
        assertEquals("10.10.10.0", LabNetwork.ofCidr("10.10.10.0/24").address());
        assertEquals("172.16.0.0", LabNetwork.ofCidr("172.16.0.0/12").address());
    }

    @Test
    void roundsDownToTheNetworkAddress() {
        // OpenVPN refuse une route dont l'adresse porte des bits hors du masque.
        LabNetwork network = LabNetwork.ofCidr("10.10.10.37/24");

        assertEquals("10.10.10.0", network.address());
        assertEquals("255.255.255.0", network.netmask());
    }

    @Test
    void handlesThePrefixZeroWithoutShiftingByThirtyTwo() {
        // Java traite un décalage de 32 comme un décalage de 0 : le cas est écrit à part.
        LabNetwork everything = LabNetwork.ofCidr("0.0.0.0/0");

        assertEquals("0.0.0.0", everything.address());
        assertEquals("0.0.0.0", everything.netmask());
    }

    @Test
    void toleratesSurroundingSpaces() {
        assertEquals("255.255.255.0", LabNetwork.ofCidr("  10.10.10.0/24  ").netmask());
    }

    @Test
    void refusesWhatIsNotACidr() {
        assertThrows(InvalidInputException.class, () -> LabNetwork.ofCidr("10.10.10.0"));
        assertThrows(InvalidInputException.class, () -> LabNetwork.ofCidr("10.10.10.0/33"));
        assertThrows(InvalidInputException.class, () -> LabNetwork.ofCidr("10.10.10.256/24"));
        assertThrows(InvalidInputException.class, () -> LabNetwork.ofCidr("10.10.10/24"));
        assertThrows(InvalidInputException.class, () -> LabNetwork.ofCidr("dix.dix/24"));
        assertThrows(InvalidInputException.class, () -> LabNetwork.ofCidr(null));
    }
}
