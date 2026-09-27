package com.labplatform.adapter.out.persistence.repository;

import com.labplatform.adapter.out.persistence.entity.ScenarioJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SpringDataScenarioRepository extends JpaRepository<ScenarioJpaEntity, Long> {

    /** Les étapes partent avec le scénario : la liste affiche leur nombre. */
    @EntityGraph(attributePaths = "steps")
    List<ScenarioJpaEntity> findAllByOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = "steps")
    List<ScenarioJpaEntity> findByPublishedTrueOrderByUpdatedAtDesc();

    @EntityGraph(attributePaths = "steps")
    Optional<ScenarioJpaEntity> findBySlug(String slug);
}
