package com.labplatform.application.port.in.analytics;

/**
 * Qui sont les comptes, et lesquels reviennent.
 *
 * @param activeUsers    comptes ayant agi pendant la fenêtre d'observation ;
 *                       « agi », pas « connecté » : ouvrir une session ne dit
 *                       rien de l'usage
 * @param retentionCohort comptes qui existaient déjà au début de la fenêtre.
 *                        Nul, la rétention n'est pas « mauvaise » : il n'y a
 *                        rien à mesurer, et l'interface doit le dire
 * @param retentionRate  part de cette cohorte qui agissait encore, en pourcentage
 * @param conversionRate part des comptes abonnés, en pourcentage
 */
public record AudienceMetrics(long users, long newUsers, long activeUsers, long dormantUsers, long proUsers,
                              int activeRate, int conversionRate, long retentionCohort, int retentionRate) {
}
