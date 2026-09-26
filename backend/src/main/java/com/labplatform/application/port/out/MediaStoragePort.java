package com.labplatform.application.port.out;

import java.io.InputStream;
import java.util.Optional;

/**
 * Rangement des fichiers téléversés. Le contenu ne passe jamais par la base :
 * une vidéo de plusieurs centaines de mégaoctets n'a rien à y faire.
 */
public interface MediaStoragePort {

    /**
     * Écrit le contenu sous cet identifiant.
     *
     * @return le nombre d'octets réellement écrits
     */
    long write(String id, InputStream content);

    /** Contenu du fichier, vide s'il a disparu du stockage. */
    Optional<StoredContent> read(String id);

    void delete(String id);

    /**
     * Fichier prêt à être servi.
     *
     * @param path chemin absolu sur le stockage, utilisé pour les requêtes par plage
     */
    record StoredContent(String path, long sizeBytes) {
    }
}
