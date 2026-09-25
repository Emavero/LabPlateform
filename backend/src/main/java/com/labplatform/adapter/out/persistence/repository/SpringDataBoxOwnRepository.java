package com.labplatform.adapter.out.persistence.repository;

import com.labplatform.adapter.out.persistence.entity.BoxOwnJpaEntity;
import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.scoring.PlayerScore;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SpringDataBoxOwnRepository extends JpaRepository<BoxOwnJpaEntity, Long> {

    List<BoxOwnJpaEntity> findByUserId(Long userId);

    boolean existsByUserIdAndBoxIdAndKind(Long userId, Long boxId, FlagKind kind);

    boolean existsByBoxIdAndKind(Long boxId, FlagKind kind);

    /**
     * Classement agrégé en base : un seul aller-retour, quel que soit le
     * nombre de joueurs. À points égaux, celui qui y est arrivé le premier
     * passe devant.
     */
    @Query("""
            select new com.labplatform.domain.scoring.PlayerScore(
                       u.id,
                       u.email,
                       cast(sum(o.points) as integer),
                       cast(count(o) as integer),
                       cast(sum(case when o.firstBlood = true then 1 else 0 end) as integer),
                       max(o.ownedAt))
            from BoxOwnJpaEntity o
            join UserJpaEntity u on u.id = o.userId
            group by u.id, u.email
            order by sum(o.points) desc, max(o.ownedAt) asc
            """)
    List<PlayerScore> topScores(Pageable pageable);
}
