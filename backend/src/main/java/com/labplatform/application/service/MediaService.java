package com.labplatform.application.service;

import com.labplatform.application.port.in.media.GetMediaUseCase;
import com.labplatform.application.port.in.media.UploadMediaUseCase;
import com.labplatform.application.port.out.MediaAssetRepositoryPort;
import com.labplatform.application.port.out.MediaStoragePort;
import com.labplatform.application.port.out.SecretGeneratorPort;
import com.labplatform.application.port.out.TransactionPort;
import com.labplatform.domain.media.MediaAsset;
import com.labplatform.domain.shared.InvalidInputException;
import com.labplatform.domain.shared.NotFoundException;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.AdminPolicy;

import java.io.InputStream;
import java.time.Clock;

/**
 * Fichiers téléversés (vidéos de cours).
 * <p>
 * L'écriture précède l'enregistrement en base, et un échec de validation
 * efface ce qui vient d'être écrit : mieux vaut un fichier orphelin qu'une
 * fiche qui pointe vers rien. La taille est vérifiée sur ce qui a réellement
 * été écrit, jamais sur ce que l'appelant a annoncé.
 */
public class MediaService implements UploadMediaUseCase, GetMediaUseCase {

    private final MediaAssetRepositoryPort assets;
    private final MediaStoragePort storage;
    private final SecretGeneratorPort secrets;
    private final TransactionPort transactions;
    private final Clock clock;
    private final long maxSizeBytes;

    public MediaService(MediaAssetRepositoryPort assets, MediaStoragePort storage, SecretGeneratorPort secrets,
                        TransactionPort transactions, Clock clock, long maxSizeBytes) {
        this.assets = assets;
        this.storage = storage;
        this.secrets = secrets;
        this.transactions = transactions;
        this.clock = clock;
        this.maxSizeBytes = maxSizeBytes;
    }

    @Override
    public MediaAsset upload(Actor actor, String filename, String contentType, long declaredSize,
                             InputStream content) {
        AdminPolicy.requireAdmin(actor);
        String type = MediaAsset.requireSupported(contentType);
        if (declaredSize > maxSizeBytes) {
            throw tooLarge();
        }

        String id = secrets.hexToken();
        long written = storage.write(id, content);
        try {
            if (written > maxSizeBytes) {
                throw tooLarge();
            }
            MediaAsset asset = new MediaAsset(id, filename, type, written, clock.instant(), actor.userId());
            return transactions.inTransaction(() -> assets.save(asset));
        } catch (RuntimeException e) {
            storage.delete(id);
            throw e;
        }
    }

    @Override
    public Media get(Actor actor, String id) {
        MediaAsset asset = assets.findById(id).orElseThrow(() -> new NotFoundException("Fichier introuvable"));
        MediaStoragePort.StoredContent content = storage.read(id)
                .orElseThrow(() -> new NotFoundException("Fichier introuvable"));
        return new Media(asset, content);
    }

    private InvalidInputException tooLarge() {
        return new InvalidInputException("Fichier trop lourd : " + (maxSizeBytes / (1024 * 1024)) + " Mo au plus");
    }
}
