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

    long countByKindAndOccurredAtAfterAndOccurredAtLessThanEqual(JournalKind kind, Instant from, Instant to);

    /** Décompte par nature : agrégé en base, pas en mémoire. */
    @Query("""
            select e.kind as kind, count(e) as total
            from JournalEventJpaEntity e
            where e.occurredAt > :from and e.occurredAt <= :to
            group by e.kind
            order by count(e) desc
            """)
    List<KindCount> tallyByKind(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select e.subject as subject, count(e) as total
            from JournalEventJpaEntity e
            where e.kind = :kind and e.occurredAt > :from and e.occurredAt <= :to and e.subject is not null
            group by e.subject
            order by count(e) desc
            """)
    List<SubjectCount> tallyBySubject(@Param("kind") JournalKind kind, @Param("from") Instant from,
                                      @Param("to") Instant to, Pageable page);

    @Query("""
            select e.userId as userId, e.kind as kind, count(e) as total
            from JournalEventJpaEntity e
            where e.occurredAt > :from and e.occurredAt <= :to
            group by e.userId, e.kind
            """)
    List<UserKindCount> activityByUser(@Param("from") Instant from, @Param("to") Instant to);

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
