package com.labplatform.application.fakes;

import com.labplatform.application.port.out.ScenarioRepositoryPort;
import com.labplatform.domain.scenario.Scenario;
import com.labplatform.domain.scenario.ScenarioStep;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Scénarios en mémoire.
 * <p>
 * Les étapes reçoivent un identifiant à l'enregistrement, comme en base : sans
 * cela, l'avancement — qui coche les étapes par leur identifiant — se
 * comporterait différemment en test et en production.
 */
public class InMemoryScenarios implements ScenarioRepositoryPort {

    private final Map<String, Scenario> store = new LinkedHashMap<>();
    private long scenarios = 0;
    private long steps = 0;

    @Override
    public List<Scenario> findAll() {
        return store.values().stream().sorted(Comparator.comparing(Scenario::getUpdatedAt).reversed()).toList();
    }

    @Override
    public List<Scenario> findPublished() {
        return findAll().stream().filter(Scenario::isPublished).toList();
    }

    @Override
    public Optional<Scenario> findBySlug(String slug) {
        return Optional.ofNullable(store.get(slug));
    }

    @Override
    public Scenario save(Scenario scenario) {
        Long id = scenario.getId() != null ? scenario.getId()
                : Optional.ofNullable(store.get(scenario.getSlug())).map(Scenario::getId).orElse(++scenarios);
        List<ScenarioStep> stored = new ArrayList<>();
        for (ScenarioStep step : scenario.getSteps()) {
            stored.add(new ScenarioStep(step.id() != null ? step.id() : ++steps, step.position(), step.kind(),
                    step.reference(), step.instruction(), step.objective()));
        }
        Scenario saved = Scenario.restore(id, scenario.getSlug(), scenario.getTitle(), scenario.getBrief(),
                scenario.isPublished(), scenario.getCreatedAt(), scenario.getUpdatedAt(), stored);
        store.put(saved.getSlug(), saved);
        return saved;
    }

    @Override
    public void delete(Scenario scenario) {
        store.remove(scenario.getSlug());
    }
}
