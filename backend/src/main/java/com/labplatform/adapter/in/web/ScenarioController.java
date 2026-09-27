package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.dto.ScenarioDtos.ScenarioResponse;
import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.scenario.ListScenariosUseCase;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Scénarios publiés, avec l'avancement du joueur connecté. */
@RestController
@RequestMapping("/api/scenarios")
public class ScenarioController {

    private final ListScenariosUseCase scenarios;

    public ScenarioController(ListScenariosUseCase scenarios) {
        this.scenarios = scenarios;
    }

    @GetMapping
    public List<ScenarioResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return scenarios.listPublished(user.toActor()).stream().map(ScenarioResponse::from).toList();
    }

    @GetMapping("/{slug}")
    public ScenarioResponse one(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String slug) {
        return ScenarioResponse.from(scenarios.get(user.toActor(), slug));
    }
}
