package com.labplatform.adapter.out.vpn;

import com.labplatform.application.port.out.LabVpnProfilePort;
import com.labplatform.config.AppProperties;
import com.labplatform.domain.shared.ServiceUnavailableException;
import com.labplatform.domain.vpn.LabVpnProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Optional;

/**
 * Le profil déposé, rangé sur le disque à côté de l'autorité de certification.
 * <p>
 * Deux fichiers : le profil lui-même, et une ligne de métadonnées qui retient
 * son nom d'origine et sa date de dépôt. Le nom compte — c'est celui que
 * l'apprenant verra dans son dossier de téléchargement — et le système de
 * fichiers ne le conserverait pas si l'on écrasait toujours le même nom.
 * <p>
 * Le dossier est celui du VPN : un volume déjà persistant et déjà hors de
 * portée du serveur web. Un profil contient une clé privée ; le ranger dans un
 * dossier servi en statique le donnerait au premier venu.
 * <p>
 * Le nom d'origine n'est jamais utilisé comme nom de fichier sur le disque :
 * seul le domaine le valide, et c'est une validation de forme, pas une
 * garantie contre une remontée de chemin.
 */
@Component
public class FilesystemLabVpnProfile implements LabVpnProfilePort {

    private static final Logger log = LoggerFactory.getLogger(FilesystemLabVpnProfile.class);

    private static final String PROFILE_FILE = "lab-profile.ovpn";
    private static final String META_FILE = "lab-profile.meta";

    private final Path directory;

    public FilesystemLabVpnProfile(AppProperties properties) {
        this.directory = Path.of(properties.getVpn().getDirectory()).resolve("uploaded");
    }

    @Override
    public void save(LabVpnProfile profile) {
        try {
            Files.createDirectories(directory);
            Path temporary = directory.resolve(PROFILE_FILE + ".tmp");
            Files.writeString(temporary, profile.content(), StandardCharsets.UTF_8);
            // Déplacement atomique : un téléchargement simultané lit l'ancien
            // profil en entier, ou le nouveau, jamais un fichier à moitié écrit.
            Files.move(temporary, directory.resolve(PROFILE_FILE), StandardCopyOption.REPLACE_EXISTING);
            Files.writeString(directory.resolve(META_FILE),
                    profile.uploadedAt().toString() + "\n" + profile.fileName() + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Échec d'écriture du profil VPN déposé dans {}", directory, e);
            throw new ServiceUnavailableException("Le profil n'a pas pu être enregistré");
        }
    }

    @Override
    public Optional<LabVpnProfile> find() {
        Path profile = directory.resolve(PROFILE_FILE);
        if (!Files.isRegularFile(profile)) {
            return Optional.empty();
        }
        try {
            String content = Files.readString(profile, StandardCharsets.UTF_8);
            return Optional.of(LabVpnProfile.of(fileName(), content, uploadedAt(profile)));
        } catch (IOException e) {
            log.error("Profil VPN déposé illisible dans {}", directory, e);
            throw new ServiceUnavailableException("Le profil déposé est illisible");
        } catch (RuntimeException e) {
            // Fichier abîmé ou remplacé à la main par autre chose : mieux vaut
            // « aucun profil » qu'une erreur à chaque téléchargement.
            log.error("Profil VPN déposé invalide dans {} : il est ignoré", directory, e);
            return Optional.empty();
        }
    }

    @Override
    public void delete() {
        try {
            Files.deleteIfExists(directory.resolve(PROFILE_FILE));
            Files.deleteIfExists(directory.resolve(META_FILE));
        } catch (IOException e) {
            log.error("Échec de suppression du profil VPN déposé dans {}", directory, e);
            throw new ServiceUnavailableException("Le profil n'a pas pu être retiré");
        }
    }

    /** Nom d'origine, ou un nom neutre si les métadonnées ont disparu. */
    private String fileName() {
        return metaLine(1).orElse("cyberMans-lab.ovpn");
    }

    /** Date de dépôt, ou celle du fichier si les métadonnées ont disparu. */
    private Instant uploadedAt(Path profile) {
        return metaLine(0).map(text -> {
            try {
                return Instant.parse(text);
            } catch (RuntimeException e) {
                return null;
            }
        }).orElseGet(() -> lastModified(profile));
    }

    private Optional<String> metaLine(int index) {
        Path meta = directory.resolve(META_FILE);
        if (!Files.isRegularFile(meta)) {
            return Optional.empty();
        }
        try {
            return Files.readAllLines(meta, StandardCharsets.UTF_8).stream()
                    .skip(index)
                    .findFirst()
                    .map(String::strip)
                    .filter(line -> !line.isBlank());
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static Instant lastModified(Path profile) {
        try {
            return Files.getLastModifiedTime(profile).toInstant();
        } catch (IOException e) {
            return Instant.EPOCH;
        }
    }
}
