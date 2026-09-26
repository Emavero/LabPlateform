package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.dto.BoxDtos.BoxResponse;
import com.labplatform.adapter.in.web.dto.BoxDtos.FlagSubmissionRequest;
import com.labplatform.adapter.in.web.dto.BoxDtos.FlagSubmissionResponse;
import com.labplatform.adapter.in.web.dto.BoxDtos.RatingRequest;
import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.box.GetBoxUseCase;
import com.labplatform.application.port.in.box.ListBoxesUseCase;
import com.labplatform.application.port.in.box.RateBoxUseCase;
import com.labplatform.application.port.in.box.SubmitFlagUseCase;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/boxes")
public class BoxController {

    private final ListBoxesUseCase listBoxes;
    private final GetBoxUseCase getBox;
    private final SubmitFlagUseCase submitFlag;
    private final RateBoxUseCase rateBox;

    public BoxController(ListBoxesUseCase listBoxes, GetBoxUseCase getBox, SubmitFlagUseCase submitFlag,
                         RateBoxUseCase rateBox) {
        this.listBoxes = listBoxes;
        this.getBox = getBox;
        this.submitFlag = submitFlag;
        this.rateBox = rateBox;
    }

    @GetMapping
    public List<BoxResponse> list(@AuthenticationPrincipal AuthenticatedUser user) {
        return listBoxes.listBoxes(user.toActor()).stream().map(BoxResponse::from).toList();
    }

    @GetMapping("/{slug}")
    public BoxResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable String slug) {
        return BoxResponse.from(getBox.getBox(user.toActor(), slug));
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
