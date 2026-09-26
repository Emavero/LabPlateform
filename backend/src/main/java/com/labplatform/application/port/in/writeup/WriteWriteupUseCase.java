package com.labplatform.application.port.in.writeup;

import com.labplatform.domain.user.Actor;

public interface WriteWriteupUseCase {

    /**
     * Enregistre le compte rendu de l'appelant, en le créant au besoin.
     *
     * @throws com.labplatform.domain.shared.ConflictException machine pas encore possédée
     */
    WriteupView save(Actor actor, String slug, String title, String content, boolean published);

    /** Supprime le compte rendu de l'appelant. Sans effet s'il n'en avait pas. */
    void delete(Actor actor, String slug);
}
