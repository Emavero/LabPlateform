package com.labplatform.application.service;

import com.labplatform.application.port.in.journal.GetJournalUseCase;
import com.labplatform.application.port.in.journal.JournalLine;
import com.labplatform.application.port.out.JournalPort;
import com.labplatform.application.port.out.UserRepositoryPort;
import com.labplatform.domain.journal.JournalEvent;
import com.labplatform.domain.scoring.Handle;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.AdminPolicy;
import com.labplatform.domain.user.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lecture du journal d'activité.
 * <p>
 * Un compte ne lit que le sien ; l'administration lit celui de la plateforme.
 * Dans les deux cas les auteurs n'apparaissent que par leur pseudonyme : un
 * journal d'exploitation n'a pas à distribuer des adresses e-mail.
 */
public class JournalService implements GetJournalUseCase {

    private final JournalPort journal;
    private final UserRepositoryPort users;

    public JournalService(JournalPort journal, UserRepositoryPort users) {
        this.journal = journal;
        this.users = users;
    }

    @Override
    public List<JournalLine> myJournal(Actor actor, int limit) {
        String handle = handleOf(actor.userId());
        return journal.findByUser(actor.userId(), limit).stream()
                .map(event -> new JournalLine(event, handle))
                .toList();
    }

    @Override
    public List<JournalLine> platformJournal(Actor actor, int limit) {
        AdminPolicy.requireAdmin(actor);
        List<JournalEvent> events = journal.findRecent(limit);
        // Un seul passage par compte : la page mélange les auteurs, et demander
        // le même compte à chaque ligne multiplierait les requêtes.
        Map<Long, String> handles = new HashMap<>();
        return events.stream()
                .map(event -> new JournalLine(event,
                        handles.computeIfAbsent(event.getUserId(), this::handleOf)))
                .toList();
    }

    private String handleOf(Long userId) {
        return users.findById(userId)
                .map(User::getEmail)
                .map(email -> Handle.fromEmail(email.value()))
                // Compte supprimé : le journal survit à son auteur.
                .orElse("compte supprimé");
    }
}
