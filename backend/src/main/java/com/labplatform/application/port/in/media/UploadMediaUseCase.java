package com.labplatform.application.port.in.media;

import com.labplatform.domain.media.MediaAsset;
import com.labplatform.domain.user.Actor;

import java.io.InputStream;

public interface UploadMediaUseCase {

    /**
     * Téléverse un fichier et renvoie sa fiche, dont l'adresse à référencer
     * dans une section de cours.
     *
     * @throws com.labplatform.domain.shared.ForbiddenException    appelant non administrateur
     * @throws com.labplatform.domain.shared.InvalidInputException format refusé ou fichier trop lourd
     */
    MediaAsset upload(Actor actor, String filename, String contentType, long declaredSize, InputStream content);
}
