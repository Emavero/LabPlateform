package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.dto.ScenarioDtos.ScenarioResponse;
import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.scenario.ManageScenariosUseCase;
import com.labplatform.application.port.in.scenario.ScenarioDraft;
import com.labplatform.domain.scenario.ScenarioObjective;
import com.labplatform.domain.scenario.ScenarioStepKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Conception des scénarios : brouillons compris, contrairement à la vue joueur. */
@RestController
@RequestMapping("/api/admin/scenarios")
public class AdminScenarioController {

    private final ManageScenariosUseCase scenarios;

    public AdminScenarioController(ManageScenariosUseCase scenarios) {
        this.scenarios = scenarios;
    }

    @GetMapping
    public List<ScenarioResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return scenarios.listAll(user.toActor()).stream().map(ScenarioResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SlugResponse create(@AuthenticationPrincipal AuthenticatedUser user,
                               @Valid @RequestBody ScenarioRequest request) {
        return new SlugResponse(scenarios.create(user.toActor(), request.toDraft()).getSlug());
    }

    @PutMapping("/{slug}")
    public SlugResponse update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String slug,
                               @Valid @RequestBody ScenarioRequest request) {
        return new SlugResponse(scenarios.update(user.toActor(), slug, request.toDraft()).getSlug());
    }

    @DeleteMapping("/{slug}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String slug) {
        scenarios.delete(user.toActor(), slug);
    }

    public record SlugResponse(String slug) {
    }

    public record ScenarioRequest(
            @NotBlank(message = "Le titre est obligatoire")
            @Size(max = 140, message = "Le titre est limité à 140 caractères") String title,
            @NotBlank(message = "La mise en situation est obligatoire")
            @Size(max = 2_000, message = "La mise en situation est limitée à 2 000 caractères") String brief,
            boolean published,
            @Valid List<StepRequest> steps) {

        ScenarioDraft toDraft() {
            return new ScenarioDraft(title, brief, published,
                    steps == null ? List.of() : steps.stream().map(StepRequest::toDraft).toList());
        }
    }

    public record StepRequest(
            @NotNull(message = "Chaque étape désigne une machine ou un cours") ScenarioStepKind kind,
            @NotBlank(message = "Chaque étape désigne une machine ou un cours") String reference,
            @Size(max = 1_000, message = "La consigne est limitée à 1 000 caractères") String instruction,
            ScenarioObjective objective) {

        ScenarioDraft.StepDraft toDraft() {
            return new ScenarioDraft.StepDraft(kind, reference, instruction, objective);
        }
    }
}
