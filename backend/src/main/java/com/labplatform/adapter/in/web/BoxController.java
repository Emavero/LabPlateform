package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.dto.BoxDtos.BoxResponse;
import com.labplatform.adapter.in.web.dto.BoxDtos.FlagSubmissionRequest;
import com.labplatform.adapter.in.web.dto.BoxDtos.FlagSubmissionResponse;
import com.labplatform.adapter.in.web.dto.BoxDtos.RatingRequest;
import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.box.GetBoxUseCase;
import com.labplatform.application.port.in.box.ListBoxesUseCase;
import com.labplatform.application.port.in.box.RateBoxUseCase;
import com.labplatform.application.port.in.box.SpawnBoxUseCase;
import com.labplatform.application.port.in.box.SubmitFlagUseCase;
import com.labplatform.application.port.out.JournalPort;
import com.labplatform.domain.journal.JournalEvent;
import com.labplatform.domain.journal.JournalKind;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.List;

@RestController
@RequestMapping("/api/boxes")
public class BoxController {

    private final ListBoxesUseCase listBoxes;
    private final GetBoxUseCase getBox;
    private final SubmitFlagUseCase submitFlag;
    private final RateBoxUseCase rateBox;
    private final SpawnBoxUseCase spawnBox;
    private final JournalPort journal;
    private final Clock clock;

    public BoxController(ListBoxesUseCase listBoxes, GetBoxUseCase getBox, SubmitFlagUseCase submitFlag,
                         RateBoxUseCase rateBox, SpawnBoxUseCase spawnBox, JournalPort journal, Clock clock) {
        this.listBoxes = listBoxes;
        this.getBox = getBox;
        this.submitFlag = submitFlag;
        this.rateBox = rateBox;
        this.spawnBox = spawnBox;
        this.journal = journal;
        this.clock = clock;
    }

    @GetMapping
    public List<BoxResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return listBoxes.listBoxes(user.toActor()).stream().map(BoxResponse::from).toList();
    }

    /**
     * Fiche d'une machine, et seule visite comptée : le cas d'usage est aussi
     * appelé pour rendre l'état après un lancement de cible ou une note, et
     * compter ces appels comme des visites faussait le classement des machines
     * les plus regardées. C'est donc ici, où l'on sait qu'une requête est bien
     * une consultation, que le journal est écrit.
     */
    @GetMapping("/{slug}")
    public BoxResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String slug) {
        var view = getBox.getBox(user.toActor(), slug);
        journal.record(JournalEvent.of(user.toActor().userId(), JournalKind.BOX_VIEWED, view.box().getSlug(),
                clock.instant()));
        return BoxResponse.from(view);
    }

    /** Lance la cible de cette machine pour l'appelant. */
    @PostMapping("/{slug}/instance")
    public BoxResponse spawn(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String slug) {
        return BoxResponse.from(spawnBox.spawn(user.toActor(), slug));
    }

    @DeleteMapping("/{slug}/instance")
    public BoxResponse stopInstance(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String slug) {
        return BoxResponse.from(spawnBox.stop(user.toActor(), slug));
    }

    /** Note de difficulté ressentie, réservée aux machines possédées. */
    @PutMapping("/{slug}/rating")
    public BoxResponse rate(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String slug,
                            @Valid @RequestBody RatingRequest request) {
        return BoxResponse.from(rateBox.rateBox(user.toActor(), slug, request.difficulty()));
    }

    @PostMapping("/{slug}/flags")
    public FlagSubmissionResponse submit(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String slug,
                                         @Valid @RequestBody FlagSubmissionRequest request) {
        return FlagSubmissionResponse.from(
                submitFlag.submitFlag(user.toActor(), slug, request.kind(), request.flag()));
    }
}
