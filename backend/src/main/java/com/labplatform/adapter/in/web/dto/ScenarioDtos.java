package com.labplatform.adapter.in.web.dto;

import com.labplatform.adapter.in.web.Texts;
import com.labplatform.application.port.in.scenario.ScenarioView;
import com.labplatform.domain.scenario.Scenario;
import com.labplatform.domain.scenario.ScenarioProgress;

import java.time.Instant;
import java.util.List;

/** Formes exposées par les scénarios d'exercice. */
public final class ScenarioDtos {

    private ScenarioDtos() {
    }

    /**
     * Étape présentée.
     *
     * @param missing la ressource visée a disparu du catalogue : l'étape est
     *                infranchissable, et l'administration doit le voir plutôt que
     *                de découvrir un scénario qui bloque
     */
    public record StepResponse(Long id, int position, String kind, String kindName, String reference, String name,
                               String instruction, String objective, String objectiveName, boolean missing,
                               boolean locked, boolean done) {

        static StepResponse from(ScenarioView.ScenarioStepView view) {
            var step = view.step();
            return new StepResponse(step.id(), step.position(), step.kind().name(),
                    Texts.of(step.kind().displayName()), step.reference(), view.name(), step.instruction(),
                    step.objective() == null ? null : step.objective().name(),
                    step.objective() == null ? null : Texts.of(step.objective().displayName()),
                    view.missing(), view.locked(), view.done());
        }
    }

    public record ProgressResponse(int done, int total, Integer nextPosition, boolean complete, int percent) {

        static ProgressResponse from(ScenarioProgress progress) {
            return new ProgressResponse(progress.done(), progress.total(), progress.nextPosition(),
                    progress.isComplete(), (int) Math.round(progress.ratio() * 100));
        }
    }

    public record ScenarioResponse(String slug, String title, String brief, boolean published, Instant updatedAt,
                                   List<StepResponse> steps, ProgressResponse progress) {

        public static ScenarioResponse from(ScenarioView view) {
            Scenario scenario = view.scenario();
            return new ScenarioResponse(scenario.getSlug(), scenario.getTitle(), scenario.getBrief(),
                    scenario.isPublished(), scenario.getUpdatedAt(),
                    view.steps().stream().map(StepResponse::from).toList(),
                    ProgressResponse.from(view.progress()));
        }
    }
}
