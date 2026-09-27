package com.labplatform.adapter.out.persistence;

import com.labplatform.adapter.out.persistence.entity.ScenarioJpaEntity;
import com.labplatform.adapter.out.persistence.entity.ScenarioStepJpaEntity;
import com.labplatform.adapter.out.persistence.repository.SpringDataScenarioRepository;
import com.labplatform.application.port.out.ScenarioRepositoryPort;
import com.labplatform.domain.scenario.Scenario;
import com.labplatform.domain.scenario.ScenarioStep;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class ScenarioPersistenceAdapter implements ScenarioRepositoryPort {

    private final SpringDataScenarioRepository repository;

    public ScenarioPersistenceAdapter(SpringDataScenarioRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Scenario> findAll() {
        return repository.findAllByOrderByUpdatedAtDesc().stream()
                .map(ScenarioPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Scenario> findPublished() {
        return repository.findByPublishedTrueOrderByUpdatedAtDesc().stream()
                .map(ScenarioPersistenceAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Scenario> findBySlug(String slug) {
        return repository.findBySlug(slug).map(ScenarioPersistenceAdapter::toDomain);
    }

    /**
     * Les étapes sont remplacées en bloc : l'éditeur envoie la liste complète, et
     * une étape n'a pas d'identité que le lecteur suivrait d'une version à
     * l'autre. La collection est vidée puis regarnie plutôt que réaffectée —
     * remplacer l'instance ferait perdre le suivi d'Hibernate.
     */
    @Override
    @Transactional
    public Scenario save(Scenario scenario) {
        ScenarioJpaEntity entity = scenario.getId() == null
                ? new ScenarioJpaEntity(null, scenario.getSlug(), scenario.getTitle(), scenario.getBrief(),
                        scenario.isPublished(), scenario.getCreatedAt(), scenario.getUpdatedAt())
                : repository.findBySlug(scenario.getSlug()).orElseThrow();
        entity.update(scenario.getTitle(), scenario.getBrief(), scenario.isPublished(), scenario.getUpdatedAt());
        entity.getSteps().clear();
        for (ScenarioStep step : scenario.getSteps()) {
            entity.getSteps().add(new ScenarioStepJpaEntity(null, step.position(), step.kind(), step.reference(),
                    step.instruction(), step.objective()));
        }
        return toDomain(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Scenario scenario) {
        repository.findBySlug(scenario.getSlug()).ifPresent(repository::delete);
    }

    private static Scenario toDomain(ScenarioJpaEntity e) {
        List<ScenarioStep> steps = e.getSteps().stream()
                .map(step -> new ScenarioStep(step.getId(), step.getPosition(), step.getKind(), step.getReference(),
                        step.getInstruction(), step.getObjective()))
                .toList();
        return Scenario.restore(e.getId(), e.getSlug(), e.getTitle(), e.getBrief(), e.isPublished(), e.getCreatedAt(),
                e.getUpdatedAt(), steps);
    }
}
