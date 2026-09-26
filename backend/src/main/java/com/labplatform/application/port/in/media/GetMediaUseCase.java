package com.labplatform.application.port.in.media;

import com.labplatform.application.port.out.MediaStoragePort;
import com.labplatform.domain.media.MediaAsset;
import com.labplatform.domain.user.Actor;

public interface GetMediaUseCase {

    /** Fichier à servir, pour un appelant connecté. */
    Media get(Actor actor, String id);

    record Media(MediaAsset asset, MediaStoragePort.StoredContent content) {
    }
}
