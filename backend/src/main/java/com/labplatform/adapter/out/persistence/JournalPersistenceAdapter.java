package com.labplatform.adapter.out.persistence;

import com.labplatform.adapter.out.persistence.entity.JournalEventJpaEntity;
import com.labplatform.adapter.out.persistence.repository.SpringDataJournalRepository;
import com.labplatform.application.port.out.JournalPort;
import com.labplatform.domain.journal.JournalEvent;
import com.labplatform.domain.journal.JournalKind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class JournalPersistenceAdapter implements JournalPort {

    private static final Logger log = LoggerFactory.getLogger(JournalPersistenceAdapter.class);

    private final SpringDataJournalRepository repository;

    public JournalPersistenceAdapter(SpringDataJournalRepository repository) {
        this.repository = repository;
    }

    /**
     * L'écriture se fait dans sa propre transaction et n'échoue jamais vers
     * l'appelant : une ligne de journal perdue ne doit pas annuler la
     * validation de flag qu'elle relate. La transaction séparée est ce qui
     * rend l'exception rattrapable — dans la transaction de l'appelant, la
     * rattraper laisserait une transaction condamnée au rollback.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(JournalEvent event) {
        try {
            // Vidé tout de suite : sans cela, une contrainte violée ne se
            // manifesterait qu'à la validation, hors de portée de ce bloc.
            repository.saveAndFlush(new JournalEventJpaEntity(null, event.getUserId(), event.getKind(),
                    event.getSubject().orElse(null), event.getDetail().orElse(null), event.getOccurredAt()));
        } catch (RuntimeException failure) {
            log.warn("Événement de journal non enregistré ({})", event.getKind(), failure);
        }
    }

    @Override
    public List<JournalEvent> findByUser(Long userId, int limit) {
        return repository.findByUserIdOrderByOccurredAtDesc(userId, PageRequest.of(0, bounded(limit))).stream()
                .map(JournalPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public List<JournalEvent> findRecent(int limit) {
        return repository.findAllByOrderByOccurredAtDesc(PageRequest.of(0, bounded(limit))).stream()
                .map(JournalPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    public List<Tally<JournalKind>> tallyByKindSince(Instant since) {
        return repository.tallyByKind(since).stream()
                .map(row -> new Tally<>(row.getKind(), row.getTotal()))
                .toList();
    }

    @Override
    public List<Tally<String>> tallyBySubjectSince(JournalKind kind, Instant since, int limit) {
        return repository.tallyBySubject(kind, since, PageRequest.of(0, bounded(limit))).stream()
                .map(row -> new Tally<>(row.getSubject(), row.getTotal()))
                .toList();
    }

    @Override
    public List<UserActivity> activityByUserSince(Instant since) {
        return repository.activityByUser(since).stream()
                .map(row -> new UserActivity(row.getUserId(), row.getKind(), row.getTotal()))
                .toList();
    }

    @Override
    public long countSince(JournalKind kind, Instant since) {
        return repository.countByKindAndOccurredAtAfter(kind, since);
    }

    /** Une page demandée à zéro ou sans borne ferait tomber la base, pas la page. */
    private static int bounded(int limit) {
        return Math.min(Math.max(limit, 1), 500);
    }

    private static JournalEvent toDomain(JournalEventJpaEntity e) {
        return JournalEvent.restore(e.getId(), e.getUserId(), e.getKind(), e.getSubject(), e.getDetail(),
                e.getOccurredAt());
    }
}
