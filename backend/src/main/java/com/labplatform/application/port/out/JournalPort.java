package com.labplatform.application.port.out;

import com.labplatform.domain.journal.JournalEvent;
import com.labplatform.domain.journal.JournalKind;

import java.time.Instant;
import java.util.List;

/**
 * Journal des actes, en écriture puis en lecture agrégée.
 * <p>
 * {@link #record} ne doit jamais faire échouer l'action qu'il relate : perdre
 * une ligne de journal est regrettable, perdre une validation de flag ne l'est
 * pas du même ordre. C'est l'implémentation qui s'en charge, pas les appelants.
 * <p>
 * Les décomptes sont demandés ici plutôt que calculés en mémoire : avec
 * plusieurs dizaines de milliers de lignes, tout charger pour en compter
 * quelques catégories coûterait bien plus que la requête correspondante.
 */
public interface JournalPort {

    void record(JournalEvent event);

    /** Événements d'un compte, les plus récents d'abord. */
    List<JournalEvent> findByUser(Long userId, int limit);

    /** Tous comptes confondus, les plus récents d'abord : vue d'administration. */
    List<JournalEvent> findRecent(int limit);

    /** Nombre d'événements par nature depuis cette date. */
    List<Tally<JournalKind>> tallyByKindSince(Instant since);

    /** Ressources les plus concernées par cette nature d'événement. */
    List<Tally<String>> tallyBySubjectSince(JournalKind kind, Instant since, int limit);

    /** Comptes ayant agi depuis cette date, une ligne par couple (compte, nature). */
    List<UserActivity> activityByUserSince(Instant since);

    long countSince(JournalKind kind, Instant since);

    record Tally<T>(T key, long count) {
    }

    record UserActivity(Long userId, JournalKind kind, long count) {
    }
}
