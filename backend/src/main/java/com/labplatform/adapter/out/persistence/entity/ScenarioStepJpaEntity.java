package com.labplatform.adapter.out.persistence.entity;

import com.labplatform.domain.scenario.ScenarioObjective;
import com.labplatform.domain.scenario.ScenarioStepKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Étape d'un scénario. La ressource visée est désignée par son lien. */
@Entity
@Table(name = "scenario_step")
public class ScenarioStepJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "position", nullable = false)
    private int position;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 16)
    private ScenarioStepKind kind;

    @Column(name = "reference", nullable = false, length = 64)
    private String reference;

    @Column(name = "instruction", columnDefinition = "text")
    private String instruction;

    @Enumerated(EnumType.STRING)
    @Column(name = "objective", length = 16)
    private ScenarioObjective objective;

    protected ScenarioStepJpaEntity() {
        // requis par JPA
    }

    public ScenarioStepJpaEntity(Long id, int position, ScenarioStepKind kind, String reference, String instruction,
                                 ScenarioObjective objective) {
        this.id = id;
        this.position = position;
        this.kind = kind;
        this.reference = reference;
        this.instruction = instruction;
        this.objective = objective;
    }

    public Long getId() {
        return id;
    }

    public int getPosition() {
        return position;
    }

    public ScenarioStepKind getKind() {
        return kind;
    }

    public String getReference() {
        return reference;
    }

    public String getInstruction() {
        return instruction;
    }

    public ScenarioObjective getObjective() {
        return objective;
    }
}
