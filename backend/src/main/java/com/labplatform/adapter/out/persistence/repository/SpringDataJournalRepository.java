package com.labplatform.adapter.out.persistence.repository;

import com.labplatform.adapter.out.persistence.entity.JournalEventJpaEntity;
import com.labplatform.domain.journal.JournalKind;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SpringDataJournalRepository extends JpaRepository<JournalEventJpaEntity, Long> {

    List<JournalEventJpaEntity> findByUserIdOrderByOccurredAtDesc(Long userId, Pageable page);

    List<JournalEventJpaEntity> findAllByOrderByOccurredAtDesc(Pageable page);

    long countByKindAndOccurredAtAfter(JournalKind kind, Instant since);

    /** Décompte par nature : agrégé en base, pas en mémoire. */
    @Query("""
            select e.kind as kind, count(e) as total
            from JournalEventJpaEntity e
            where e.occurredAt > :since
            group by e.kind
            order by count(e) desc
            """)
    List<KindCount> tallyByKind(@Param("since") Instant since);

    @Query("""
            select e.subject as subject, count(e) as total
            from JournalEventJpaEntity e
            where e.kind = :kind and e.occurredAt > :since and e.subject is not null
            group by e.subject
            order by count(e) desc
            """)
    List<SubjectCount> tallyBySubject(@Param("kind") JournalKind kind, @Param("since") Instant since, Pageable page);

    @Query("""
            select e.userId as userId, e.kind as kind, count(e) as total
            from JournalEventJpaEntity e
            where e.occurredAt > :since
            group by e.userId, e.kind
            """)
    List<UserKindCount> activityByUser(@Param("since") Instant since);

    /** Projections : Spring Data remplit ces interfaces depuis les colonnes nommées. */
    interface KindCount {
        JournalKind getKind();

        long getTotal();
    }

    interface SubjectCount {
        String getSubject();

        long getTotal();
    }

    interface UserKindCount {
        Long getUserId();

        JournalKind getKind();

        long getTotal();
    }
}
