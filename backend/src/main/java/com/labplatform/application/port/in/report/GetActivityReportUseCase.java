package com.labplatform.application.port.in.report;

import com.labplatform.domain.report.ActivityReport;
import com.labplatform.domain.user.Actor;

public interface GetActivityReportUseCase {

    /**
     * Rapport d'activité de l'appelant sur les {@code days} derniers jours.
     * <p>
     * Le rapport ne parle que de celui qui le demande : l'activité d'un autre
     * compte n'est pas une donnée qui se prête, et l'administration a son propre
     * tableau de bord pour la vue d'ensemble.
     */
    ActivityReport report(Actor actor, int days);
}
