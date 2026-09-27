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

    /**
     * Décomptes sur un intervalle, borne inférieure exclue et borne supérieure
     * incluse.
     * <p>
     * Les deux bornes sont demandées, et non une simple date de départ : le
     * tableau de bord compare une période à celle qui la précède, ce qu'une
     * borne unique ne permet pas — on ne peut pas soustraire deux « depuis »
     * sans compter deux fois ce qui est à cheval.
     */
    List<Tally<JournalKind>> tallyByKindBetween(Instant from, Instant to);

    /** Ressources les plus concernées par cette nature d'événement. */
    List<Tally<String>> tallyBySubjectBetween(JournalKind kind, Instant from, Instant to, int limit);

    /**
     * Événements d'un compte sur un intervalle, du plus ancien au plus récent.
     * <p>
     * Demandé au journal plutôt qu'obtenu en tronquant {@link #findByUser} :
     * un rapport qui compte doit voir toute la période, et une limite laisserait
     * silencieusement tomber les premiers jours d'un compte très actif.
     */
    List<JournalEvent> findByUserBetween(Long userId, Instant from, Instant to);

    /** Comptes ayant agi sur l'intervalle, une ligne par couple (compte, nature). */
    List<UserActivity> activityByUserBetween(Instant from, Instant to);

    long countBetween(JournalKind kind, Instant from, Instant to);

    record Tally<T>(T key, long count) {
    }

    record UserActivity(Long userId, JournalKind kind, long count) {
    }
}
