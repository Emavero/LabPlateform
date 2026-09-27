package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.dto.ExposureDtos.ExposureResponse;
import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.exposure.GetLabExposureUseCase;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Surface d'attaque du lab : l'analyse d'exposition et les chemins de
 * progression sortent du même calcul, donc de la même route — les demander
 * séparément ferait refaire deux fois le même travail.
 */
@RestController
@RequestMapping("/api/exposure")
public class ExposureController {

    private final GetLabExposureUseCase exposure;

    public ExposureController(GetLabExposureUseCase exposure) {
        this.exposure = exposure;
    }

    @GetMapping
    public ExposureResponse exposure(@AuthenticationPrincipal AuthenticatedUser user) {
        return ExposureResponse.from(exposure.exposure(user.toActor()));
    }
}
