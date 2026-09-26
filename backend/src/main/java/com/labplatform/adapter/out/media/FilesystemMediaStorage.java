package com.labplatform.adapter.out.media;

import com.labplatform.application.port.out.MediaStoragePort;
import com.labplatform.config.AppProperties;
import com.labplatform.domain.shared.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Fichiers rangés à plat dans un dossier, nommés par leur identifiant.
 * <p>
 * Ce dossier est un volume à sauvegarder : le perdre laisse des cours qui
 * pointent vers des vidéos disparues. L'identifiant est validé avant de
 * toucher au disque, ce qui ferme toute remontée de chemin.
 */
@Component
public class FilesystemMediaStorage implements MediaStoragePort {

    private static final Logger log = LoggerFactory.getLogger(FilesystemMediaStorage.class);
    private static final Pattern ID_FORMAT = Pattern.compile("^[0-9a-f]{32}$");

    private final Path directory;

    public FilesystemMediaStorage(AppProperties properties) {
        this.directory = Path.of(properties.getMedia().getDirectory());
    }

    @Override
    public long write(String id, InputStream content) {
        Path target = resolve(id);
        try {
            Files.createDirectories(directory);
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
            return Files.size(target);
        } catch (IOException e) {
            log.error("Échec d'écriture du fichier {}", id, e);
            throw new ServiceUnavailableException("Le fichier n'a pas pu être enregistré sur le serveur.");
        }
    }

    @Override
    public Optional<StoredContent> read(String id) {
        Path source = resolve(id);
        if (!Files.isRegularFile(source)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new StoredContent(source.toString(), Files.size(source)));
        } catch (IOException e) {
            log.error("Échec de lecture du fichier {}", id, e);
            return Optional.empty();
        }
    }

    @Override
    public void delete(String id) {
        try {
            Files.deleteIfExists(resolve(id));
        } catch (IOException e) {
            // Un fichier orphelin est gênant, pas bloquant : l'appelant a déjà son erreur.
            log.warn("Échec de suppression du fichier {}", id, e);
        }
    }

    /** Aucun identifiant douteux n'atteint le système de fichiers. */
    private Path resolve(String id) {
        if (id == null || !ID_FORMAT.matcher(id).matches()) {
            throw new IllegalArgumentException("Identifiant de fichier invalide");
        }
        return directory.resolve(id);
    }
}
