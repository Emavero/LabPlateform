package com.labplatform.adapter.out.vpn;

import com.labplatform.adapter.out.process.CommandRunner;
import com.labplatform.application.port.out.VpnCertificateAuthorityPort;
import com.labplatform.domain.shared.ServiceUnavailableException;
import com.labplatform.domain.vpn.VpnProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Autorité de certification du VPN reposant sur easy-rsa, l'outil de référence
 * d'OpenVPN. La PKI est créée automatiquement au premier usage : AC, certificat
 * du serveur (à copier sur la passerelle), liste de révocation et clé tls-crypt.
 * <p>
 * easy-rsa ne supporte pas les appels concurrents sur une même PKI : toutes les
 * opérations sont sérialisées.
 */
public class EasyRsaCertificateAuthority implements VpnCertificateAuthorityPort {

    private static final Logger log = LoggerFactory.getLogger(EasyRsaCertificateAuthority.class);

    private final EasyRsaSettings settings;
    private final CommandRunner commands;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public EasyRsaCertificateAuthority(EasyRsaSettings settings, CommandRunner commands, Clock clock) {
        this.settings = settings;
        this.commands = commands;
        this.clock = clock;
    }

    @Override
    public synchronized ClientCredentials issueClient(String commonName) {
        requireValidName(commonName);
        ensureInitialized();
        easyrsa("build-client-full", commonName, "nopass");
        return readClient(commonName).orElseThrow(() -> failure("certificat client absent après émission"));
    }

    @Override
    public synchronized void revokeClient(String commonName) {
        requireValidName(commonName);
        ensureInitialized();
        if (!Files.exists(issued(commonName))) {
            return;
        }
        easyrsa("revoke", commonName);
        easyrsa("gen-crl");
        log.info("[vpn] Certificat {} révoqué", commonName);
    }

    @Override
    public synchronized Optional<ClientCredentials> findClient(String commonName) {
        requireValidName(commonName);
        ensureInitialized();
        return readClient(commonName);
    }

    @Override
    public synchronized String caCertificate() {
        ensureInitialized();
        return read(settings.pki().resolve("ca.crt"));
    }

    @Override
    public synchronized String tlsCryptKey() {
        ensureInitialized();
        return read(settings.tlsCryptKey());
    }

    @Override
    public synchronized String revocationList() {
        ensureInitialized();
        Path crl = settings.pki().resolve("crl.pem");
        // La liste a une date d'expiration : une liste périmée ferait refuser tous les clients
        // par OpenVPN. On la régénère donc régulièrement, à la demande de la passerelle.
        if (isOlderThanRefresh(crl)) {
            easyrsa("gen-crl");
        }
        return read(crl);
    }

    /** Crée la PKI au premier usage. Ne supprime jamais une PKI existante. */
    private void ensureInitialized() {
        Path pki = settings.pki();
        if (Files.exists(pki.resolve("private").resolve("ca.key"))) {
            ensureTlsCryptKey();
            return;
        }
        if (Files.exists(pki)) {
            throw failure("la PKI " + pki + " existe mais ne contient pas d'autorité de certification");
        }
        try {
            Files.createDirectories(settings.directory());
        } catch (IOException e) {
            throw failure("impossible de créer " + settings.directory());
        }
        log.info("[vpn] Création de l'autorité de certification dans {}", pki);
        easyrsa("init-pki");
        easyrsa("--req-cn=" + settings.caCommonName(), "build-ca", "nopass");
        easyrsa("build-server-full", settings.serverCommonName(), "nopass");
        easyrsa("gen-crl");
        ensureTlsCryptKey();
        log.info("[vpn] AC prête. Fichiers à copier sur la passerelle : {}/ca.crt, {}/issued/{}.crt, "
                        + "{}/private/{}.key, {}", pki, pki, settings.serverCommonName(), pki,
                settings.serverCommonName(), settings.tlsCryptKey());
    }

    private void ensureTlsCryptKey() {
        Path key = settings.tlsCryptKey();
        if (Files.exists(key)) {
            return;
        }
        try {
            Files.writeString(key, OpenVpnStaticKey.generate(random), StandardCharsets.US_ASCII);
            restrictToOwner(key);
        } catch (IOException e) {
            throw failure("impossible d'écrire la clé tls-crypt");
        }
    }

    private Optional<ClientCredentials> readClient(String commonName) {
        Path certificate = issued(commonName);
        Path key = settings.pki().resolve("private").resolve(commonName + ".key");
        if (!Files.exists(certificate) || !Files.exists(key)) {
            return Optional.empty();
        }
        return Optional.of(new ClientCredentials(read(certificate), read(key)));
    }

    private Path issued(String commonName) {
        return settings.pki().resolve("issued").resolve(commonName + ".crt");
    }

    private boolean isOlderThanRefresh(Path file) {
        try {
            Instant modified = Files.getLastModifiedTime(file).toInstant();
            return modified.plus(settings.crlRefresh()).isBefore(clock.instant());
        } catch (IOException e) {
            return true;
        }
    }

    private void easyrsa(String... args) {
        List<String> command = new ArrayList<>();
        command.add(settings.easyrsaBinary());
        command.add("--batch");
        command.add("--pki-dir=" + settings.pki());
        command.addAll(List.of(args));
        CommandRunner.Result result = commands.run(command, null, settings.commandTimeout());
        if (!result.succeeded()) {
            log.error("[vpn] easy-rsa {} a échoué ({}) : {}", args[args.length - 1], result.exitCode(),
                    result.stderr().strip());
            throw failure("l'opération easy-rsa « " + String.join(" ", args) + " » a échoué");
        }
    }

    private static void requireValidName(String commonName) {
        if (!VpnProfile.isValidCommonName(commonName)) {
            throw new IllegalArgumentException("Nom de certificat refusé : " + commonName);
        }
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void restrictToOwner(Path file) {
        try {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException | IOException ignored) {
            // Système de fichiers non POSIX (Windows) : les droits par défaut s'appliquent.
        }
    }

    private static ServiceUnavailableException failure(String detail) {
        log.error("[vpn] Autorité de certification indisponible : {}", detail);
        return new ServiceUnavailableException("Le service VPN est momentanément indisponible.");
    }
}
