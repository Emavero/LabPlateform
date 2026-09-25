package com.labplatform.application.service;

import com.labplatform.application.port.in.vpn.DownloadVpnProfileUseCase;
import com.labplatform.application.port.in.vpn.GetVpnAccessUseCase;
import com.labplatform.application.port.in.vpn.GetVpnRevocationListUseCase;
import com.labplatform.application.port.in.vpn.RegenerateVpnProfileUseCase;
import com.labplatform.application.port.in.vpn.VpnAccess;
import com.labplatform.application.port.in.vpn.VpnProfileFile;
import com.labplatform.application.port.out.SecretGeneratorPort;
import com.labplatform.application.port.out.TransactionPort;
import com.labplatform.application.port.out.VpnCertificateAuthorityPort;
import com.labplatform.application.port.out.VpnCertificateAuthorityPort.ClientCredentials;
import com.labplatform.application.port.out.VpnProfileRepositoryPort;
import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.shared.ServiceUnavailableException;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.vpn.VpnEndpoint;
import com.labplatform.domain.vpn.VpnProfile;
import com.labplatform.domain.vpn.VpnProtocol;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Accès VPN façon Hack The Box : chaque utilisateur télécharge un profil
 * .ovpn personnel (UDP ou TCP), émis à la demande et régénérable.
 */
public class VpnService implements GetVpnAccessUseCase, DownloadVpnProfileUseCase,
        RegenerateVpnProfileUseCase, GetVpnRevocationListUseCase {

    private static final int SUFFIX_LENGTH = 10;

    private final VpnProfileRepositoryPort profiles;
    private final VpnCertificateAuthorityPort authority;
    private final SecretGeneratorPort secrets;
    private final TransactionPort transactions;
    private final Clock clock;
    private final VpnSettings settings;
    private final OvpnProfileRenderer renderer = new OvpnProfileRenderer();
    /** Deux téléchargements simultanés du même utilisateur ne doivent émettre qu'un certificat. */
    private final ConcurrentMap<Long, Object> userLocks = new ConcurrentHashMap<>();

    public VpnService(VpnProfileRepositoryPort profiles, VpnCertificateAuthorityPort authority,
                      SecretGeneratorPort secrets, TransactionPort transactions, Clock clock, VpnSettings settings) {
        this.profiles = profiles;
        this.authority = authority;
        this.secrets = secrets;
        this.transactions = transactions;
        this.clock = clock;
        this.settings = settings;
    }

    @Override
    public VpnAccess getAccess(Actor actor) {
        if (!settings.enabled()) {
            return new VpnAccess(false, Optional.empty(), List.of(), settings.labNetwork());
        }
        return accessOf(profiles.findByUserId(actor.userId()));
    }

    @Override
    public VpnProfileFile download(Actor actor, VpnProtocol protocol) {
        requireEnabled();
        VpnEndpoint endpoint = settings.endpoints().stream()
                .filter(e -> e.protocol() == protocol)
                .findFirst()
                .orElseThrow(() -> new InvalidInputException(
                        "Le VPN n'est pas proposé en " + protocol.name() + " sur cette plateforme"));

        synchronized (lockFor(actor.userId())) {
            Optional<VpnProfile> existing = profiles.findByUserId(actor.userId());
            Optional<ClientCredentials> credentials = existing.flatMap(p -> authority.findClient(p.getCommonName()));
            VpnProfile profile;
            ClientCredentials client;
            if (existing.isPresent() && credentials.isPresent()) {
                profile = existing.get();
                client = credentials.get();
            } else {
                // Premier téléchargement, ou certificat perdu (AC réinitialisée) : on en émet un nouveau.
                profile = issueFor(actor.userId());
                client = authority.findClient(profile.getCommonName())
                        .orElseThrow(() -> new ServiceUnavailableException("Certificat VPN introuvable après émission"));
            }
            String content = renderer.render(profile.getCommonName(), endpoint, authority.caCertificate(),
                    client, authority.tlsCryptKey());
            return new VpnProfileFile("cyberMans-lab-" + protocol.parameter() + ".ovpn", content);
        }
    }

    @Override
    public VpnAccess regenerate(Actor actor) {
        requireEnabled();
        synchronized (lockFor(actor.userId())) {
            profiles.findByUserId(actor.userId())
                    .ifPresent(old -> authority.revokeClient(old.getCommonName()));
            return accessOf(Optional.of(issueFor(actor.userId())));
        }
    }

    @Override
    public String revocationList() {
        requireEnabled();
        return authority.revocationList();
    }

    private VpnProfile issueFor(Long userId) {
        String commonName = VpnProfile.commonNameFor(userId, randomSuffix());
        authority.issueClient(commonName);
        VpnProfile profile = VpnProfile.issued(userId, commonName, clock.instant());
        return transactions.inTransaction(() -> profiles.save(profile));
    }

    private String randomSuffix() {
        String token = secrets.urlSafeToken().replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
        if (token.length() < SUFFIX_LENGTH) {
            throw new IllegalStateException("Jeton aléatoire trop court");
        }
        return token.substring(0, SUFFIX_LENGTH);
    }

    private VpnAccess accessOf(Optional<VpnProfile> profile) {
        return new VpnAccess(true, profile.map(VpnProfile::getIssuedAt), settings.endpoints(), settings.labNetwork());
    }

    private void requireEnabled() {
        if (!settings.enabled()) {
            throw new ServiceUnavailableException("L'accès VPN n'est pas encore configuré sur cette plateforme.");
        }
    }

    private Object lockFor(Long userId) {
        return userLocks.computeIfAbsent(userId, id -> new Object());
    }
}
