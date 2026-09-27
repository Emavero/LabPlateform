package com.labplatform.application.port.in.analytics;

import com.labplatform.domain.user.Actor;

public interface GetAdminAnalyticsUseCase {

    /**
     * Indicateurs de la plateforme sur les {@code windowDays} derniers jours.
     * Réservé à l'administration.
     */
    AdminAnalytics analytics(Actor actor, int windowDays);
}
