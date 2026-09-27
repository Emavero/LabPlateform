package com.labplatform.adapter.in.web;

import com.labplatform.adapter.in.web.dto.AnalyticsDtos;
import com.labplatform.adapter.in.web.security.AuthenticatedUser;
import com.labplatform.application.port.in.analytics.GetAdminAnalyticsUseCase;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Indicateurs de la plateforme, pour le tableau de bord d'administration. */
@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    private final GetAdminAnalyticsUseCase analytics;

    public AdminAnalyticsController(GetAdminAnalyticsUseCase analytics) {
        this.analytics = analytics;
    }

    /**
     * @param windowDays fenêtre d'observation. Tous les chiffres s'y rapportent :
     *                   des totaux depuis l'origine flatteraient une plateforme
     *                   dont personne ne se sert plus.
     */
    @GetMapping
    public AnalyticsDtos.AnalyticsResponse analytics(@AuthenticationPrincipal AuthenticatedUser user,
                                                     @RequestParam(defaultValue = "30") int windowDays) {
        return AnalyticsDtos.AnalyticsResponse.from(analytics.analytics(user.toActor(), windowDays));
    }
}
