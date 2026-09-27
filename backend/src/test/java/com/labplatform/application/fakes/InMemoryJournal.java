package com.labplatform.application.fakes;

import com.labplatform.application.port.out.JournalPort;
import com.labplatform.domain.journal.JournalEvent;
import com.labplatform.domain.journal.JournalKind;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InMemoryJournal implements JournalPort {

    public final List<JournalEvent> events = new ArrayList<>();
    private long sequence = 0;

    @Override
    public void record(JournalEvent event) {
        events.add(JournalEvent.restore(++sequence, event.getUserId(), event.getKind(),
                event.getSubject().orElse(null), event.getDetail().orElse(null), event.getOccurredAt()));
    }

    /** Natures inscrites pour ce compte, dans l'ordre d'écriture : lisible dans un test. */
    public List<JournalKind> kindsOf(Long userId) {
        return events.stream()
                .filter(event -> event.getUserId().equals(userId))
                .map(JournalEvent::getKind)
                .toList();
    }

    @Override
    public List<JournalEvent> findByUser(Long userId, int limit) {
        return events.stream()
                .filter(event -> event.getUserId().equals(userId))
                .sorted(Comparator.comparing(JournalEvent::getOccurredAt).reversed())
                .limit(limit)
                .toList();
    }

    /** Mêmes bornes que la requête : inférieure exclue, supérieure incluse. */
    @Override
    public List<JournalEvent> findByUserBetween(Long userId, Instant from, Instant to) {
        return events.stream()
                .filter(event -> event.getUserId().equals(userId))
                .filter(event -> event.getOccurredAt().isAfter(from) && !event.getOccurredAt().isAfter(to))
                .sorted(java.util.Comparator.comparing(JournalEvent::getOccurredAt))
                .toList();
    }

    @Override
    public List<JournalEvent> findRecent(int limit) {
        return events.stream()
                .sorted(Comparator.comparing(JournalEvent::getOccurredAt).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public List<Tally<JournalKind>> tallyByKindBetween(Instant from, Instant to) {
        Map<JournalKind, Long> counts = new LinkedHashMap<>();
        events.stream()
                .filter(event -> within(event.getOccurredAt(), from, to))
                .forEach(event -> counts.merge(event.getKind(), 1L, Long::sum));
        return counts.entrySet().stream()
                .map(entry -> new Tally<>(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong((Tally<JournalKind> tally) -> tally.count()).reversed())
                .toList();
    }

    @Override
    public List<Tally<String>> tallyBySubjectBetween(JournalKind kind, Instant from, Instant to, int limit) {
        Map<String, Long> counts = new LinkedHashMap<>();
        events.stream()
                .filter(event -> event.getKind() == kind && within(event.getOccurredAt(), from, to))
                .forEach(event -> event.getSubject().ifPresent(subject -> counts.merge(subject, 1L, Long::sum)));
        return counts.entrySet().stream()
                .map(entry -> new Tally<>(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong((Tally<String> tally) -> tally.count()).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public List<UserActivity> activityByUserBetween(Instant from, Instant to) {
        Map<String, UserActivity> rows = new LinkedHashMap<>();
        events.stream()
                .filter(event -> within(event.getOccurredAt(), from, to))
                .forEach(event -> rows.merge(event.getUserId() + ":" + event.getKind(),
                        new UserActivity(event.getUserId(), event.getKind(), 1L),
                        (existing, one) -> new UserActivity(existing.userId(), existing.kind(),
                                existing.count() + 1)));
        return List.copyOf(rows.values());
    }

    @Override
    public long countBetween(JournalKind kind, Instant from, Instant to) {
        return events.stream()
                .filter(event -> event.getKind() == kind && within(event.getOccurredAt(), from, to))
                .count();
    }

    /** Borne inférieure exclue, borne supérieure incluse : comme la requête réelle. */
    private static boolean within(Instant at, Instant from, Instant to) {
        return at.isAfter(from) && !at.isAfter(to);
    }
}
