package com.labplatform.application.port.in.box;

import com.labplatform.domain.user.Actor;

import java.util.List;

public interface ListBoxesUseCase {

    /** Catalogue complet, enrichi de ce que l'appelant a déjà validé. */
    List<BoxView> listBoxes(Actor actor);
}
