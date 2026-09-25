package com.labplatform.application.service;

import com.labplatform.application.fakes.Fakes;
import com.labplatform.application.port.in.vpn.VpnAccess;
import com.labplatform.application.port.in.vpn.VpnProfileFile;
import com.labplatform.application.port.out.VpnCertificateAuthorityPort;
import com.labplatform.application.port.out.VpnProfileRepositoryPort;
import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.shared.ServiceUnavailableException;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.Role;
import com.labplatform.domain.vpn.VpnEndpoint;
import com.labplatform.domain.vpn.VpnProfile;
import com.labplatform.domain.vpn.VpnProtocol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VpnServiceTest {

    private static final Actor ALICE = new Actor(1L, Role.USER);
    private static final Actor BOB = new Actor(2L, Role.USER);
    private static final VpnEndpoint UDP = new VpnEndpoint(VpnProtocol.UDP, "vpn.example.org", 1194);

    private FakeAuthority authority;
    private InMemoryProfiles profiles;
    private VpnService service;
    private int tokenCounter;

    @BeforeEach
    void setUp() {
        authority = new FakeAuthority();
        profiles = new InMemoryProfiles();
        tokenCounter = 0;
        service = service(new VpnSettings(true, List.of(UDP), "10.10.10.0/24"));
    }

    private VpnService service(VpnSettings settings) {
        return new VpnService(profiles, authority, Fakes.secretGenerator(() -> "Tok_en-" + (++tokenCounter) + "abcdefghijkl"),
                Fakes.NO_TRANSACTION, Clock.fixed(Instant.parse("2026-09-25T10:00:00Z"), ZoneOffset.UTC), settings);
    }

    @Test
    void premierTelechargementEmetUnCertificatPersonnel() {
        VpnProfileFile file = service.download(ALICE, VpnProtocol.UDP);

        assertEquals("cyberMans-lab-udp.ovpn", file.fileName());
        assertTrue(file.content().contains("remote vpn.example.org 1194"));
        assertTrue(file.content().contains("<key>\nKEY-" + profiles.get(1L).getCommonName()));
        assertEquals(1, authority.issued.size());
        assertTrue(profiles.get(1L).getCommonName().startsWith("cyberMans-u1-"));
    }

    @Test
    void telechargerDeNouveauRenvoieLeMemeCertificat() {
        String first = service.download(ALICE, VpnProtocol.UDP).content();
        String second = service.download(ALICE, VpnProtocol.UDP).content();

        assertEquals(first, second);
        assertEquals(1, authority.issued.size());
    }

    @Test
    void chaqueUtilisateurASonPropreCertificat() {
        service.download(ALICE, VpnProtocol.UDP);
        service.download(BOB, VpnProtocol.UDP);

        assertFalse(profiles.get(1L).getCommonName().equals(profiles.get(2L).getCommonName()));
    }

    @Test
    void regenererRevoqueLAncienCertificatEtEnEmetUnNouveau() {
        service.download(ALICE, VpnProtocol.UDP);
        String old = profiles.get(1L).getCommonName();

        VpnAccess access = service.regenerate(ALICE);

        assertEquals(List.of(old), authority.revoked);
        assertFalse(old.equals(profiles.get(1L).getCommonName()));
        assertTrue(access.issuedAt().isPresent());
    }

    @Test
    void certificatPerduEstReemis() {
        service.download(ALICE, VpnProtocol.UDP);
        authority.credentials.clear(); // AC réinitialisée

        service.download(ALICE, VpnProtocol.UDP);

        assertEquals(2, authority.issued.size());
    }

    @Test
    void protocoleNonPropose() {
        assertThrows(InvalidInputException.class, () -> service.download(ALICE, VpnProtocol.TCP));
    }

    @Test
    void vpnDesactive() {
        VpnService disabled = service(new VpnSettings(false, List.of(), "10.10.10.0/24"));

        assertFalse(disabled.getAccess(ALICE).enabled());
        assertThrows(ServiceUnavailableException.class, () -> disabled.download(ALICE, VpnProtocol.UDP));
        assertThrows(ServiceUnavailableException.class, () -> disabled.regenerate(ALICE));
    }

    @Test
    void accesAvantEtApresLePremierTelechargement() {
        assertFalse(service.getAccess(ALICE).issuedAt().isPresent());

        service.download(ALICE, VpnProtocol.UDP);

        assertEquals(Optional.of(Instant.parse("2026-09-25T10:00:00Z")), service.getAccess(ALICE).issuedAt());
    }

    @Test
    void leCertificatClientNeContientQueLeBlocPem() {
        String pem = "Certificate:\n    Data: ...\n-----BEGIN CERTIFICATE-----\nABC\n-----END CERTIFICATE-----\n";

        assertEquals("-----BEGIN CERTIFICATE-----\nABC\n-----END CERTIFICATE-----",
                OvpnProfileRenderer.certificateOnly(pem));
    }

    private static final class FakeAuthority implements VpnCertificateAuthorityPort {
        final List<String> issued = new ArrayList<>();
        final List<String> revoked = new ArrayList<>();
        final Map<String, ClientCredentials> credentials = new HashMap<>();

        @Override
        public ClientCredentials issueClient(String commonName) {
            issued.add(commonName);
            ClientCredentials c = new ClientCredentials(
                    "text\n-----BEGIN CERTIFICATE-----\nCERT-" + commonName + "\n-----END CERTIFICATE-----\n",
                    "KEY-" + commonName);
            credentials.put(commonName, c);
            return c;
        }

        @Override
        public void revokeClient(String commonName) {
            revoked.add(commonName);
            credentials.remove(commonName);
        }

        @Override
        public Optional<ClientCredentials> findClient(String commonName) {
            return Optional.ofNullable(credentials.get(commonName));
        }

        @Override
        public String caCertificate() {
            return "CA";
        }

        @Override
        public String tlsCryptKey() {
            return "TA";
        }

        @Override
        public String revocationList() {
            return "CRL";
        }
    }

    private static final class InMemoryProfiles implements VpnProfileRepositoryPort {
        private final Map<Long, VpnProfile> byUser = new HashMap<>();

        VpnProfile get(Long userId) {
            return byUser.get(userId);
        }

        @Override
        public Optional<VpnProfile> findByUserId(Long userId) {
            return Optional.ofNullable(byUser.get(userId));
        }

        @Override
        public VpnProfile save(VpnProfile profile) {
            byUser.put(profile.getUserId(), profile);
            return profile;
        }
    }
}
