package com.labplatform.adapter.in.event;

import com.labplatform.application.port.out.JournalPort;
import com.labplatform.domain.journal.JournalEvent;
import com.labplatform.domain.journal.JournalKind;
import com.labplatform.domain.user.UserRegistered;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Clock;

/**
 * Inscrit au journal ce que les événements du domaine annoncent déjà.
 * <p>
 * Passer par l'événement évite d'ajouter une dépendance au service
 * d'authentification, dont ce n'est pas le métier : c'est le contexte qui
 * écoute qui décide d'en garder trace.
 * <p>
 * L'écriture a lieu <em>après</em> la validation de la transaction : la ligne
 * de journal référence le compte, qui n'existe pas encore tant que
 * l'inscription n'est pas validée. Et une inscription annulée ne doit pas
 * laisser de trace d'un compte qui n'a jamais été créé.
 */
@Component
public class JournalRecordingListener {

    private final JournalPort journal;
    private final Clock clock;

    public JournalRecordingListener(JournalPort journal, Clock clock) {
        this.journal = journal;
        this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserRegistered event) {
        journal.record(JournalEvent.of(event.userId(), JournalKind.REGISTERED, null, clock.instant()));
    }
}
