package com.labplatform.application.port.out;

import com.labplatform.domain.scenario.Scenario;

import java.util.List;
import java.util.Optional;

public interface ScenarioRepositoryPort {

    /** Tous les scénarios, publiés ou non, les plus récemment modifiés d'abord. */
    List<Scenario> findAll();

    List<Scenario> findPublished();

    Optional<Scenario> findBySlug(String slug);

    Scenario save(Scenario scenario);

    void delete(Scenario scenario);
}
