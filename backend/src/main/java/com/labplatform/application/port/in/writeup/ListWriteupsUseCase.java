package com.labplatform.application.port.in.writeup;

import com.labplatform.domain.user.Actor;

import java.util.List;

public interface ListWriteupsUseCase {

    /**
     * Comptes rendus lisibles par l'appelant pour cette machine : le sien, et
     * ceux que d'autres ont publiés s'il l'a possédée lui aussi.
     */
    List<WriteupView> listWriteups(Actor actor, String slug);
}
