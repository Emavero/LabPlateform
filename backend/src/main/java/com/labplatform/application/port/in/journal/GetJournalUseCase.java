package com.labplatform.application.port.in.journal;

import com.labplatform.domain.user.Actor;

import java.util.List;

public interface GetJournalUseCase {

    /** Journal de l'appelant, le plus récent d'abord. */
    List<JournalLine> myJournal(Actor actor, int limit);

    /** Journal de toute la plateforme. Réservé à l'administration. */
    List<JournalLine> platformJournal(Actor actor, int limit);
}
