package com.labplatform.adapter.out.persistence.repository;

import com.labplatform.adapter.out.persistence.entity.BoxRatingJpaEntity;
import com.labplatform.application.port.out.BoxRatingRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SpringDataBoxRatingRepository extends JpaRepository<BoxRatingJpaEntity, Long> {

    Optional<BoxRatingJpaEntity> findByUserIdAndBoxId(Long userId, Long boxId);

    /** Dépouillement en base : une ligne par (machine, difficulté) votée. */
    @Query("""
            select new com.labplatform.application.port.out.BoxRatingRepositoryPort$Tally(
                       r.boxId, r.difficulty, cast(count(r) as integer))
            from BoxRatingJpaEntity r
            group by r.boxId, r.difficulty
            """)
    List<BoxRatingRepositoryPort.Tally> tallies();
}
