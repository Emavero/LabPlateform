package com.labplatform.application.port.in.admin;

import com.labplatform.domain.box.Box;
import com.labplatform.domain.user.Actor;

import java.util.List;

public interface ManageBoxesUseCase {

    /** Catalogue complet, machines retirées comprises. */
    List<Box> listBoxes(Actor actor);

    /**
     * Publie une machine. Son identifiant d'URL est dérivé du nom.
     *
     * @throws com.labplatform.domain.shared.ForbiddenException appelant non administrateur
     * @throws com.labplatform.domain.shared.ConflictException  une machine porte déjà ce nom
     */
    PublishedBox createBox(Actor actor, BoxDraft draft);

    /** Met à jour une machine. Son identifiant d'URL ne change pas. */
    PublishedBox updateBox(Actor actor, String slug, BoxDraft draft);

    /** Supprime une machine, ses flags validés, ses notes et ses instances. */
    void deleteBox(Actor actor, String slug);
}
