package com.labplatform.application.port.in.analytics;

import com.labplatform.domain.insight.Recommendation;

import java.time.Instant;
import java.util.List;

/**
 * Tout ce que le tableau de bord d'administration affiche, pour une fenêtre
 * d'observation donnée.
 * <p>
 * L'ordre des champs est celui de la lecture : combien de monde, ce qu'ils
 * font, ce que ça rapporte, qui ils sont, où ça coince, et quoi faire.
 *
 * @param windowDays    longueur de la fenêtre, en jours : tous les chiffres s'y rapportent
 * @param previous      les mêmes chiffres sur la période précédente, de même
 *                      longueur : c'est la comparaison qui fait la tendance
 * @param insights      classements de contenus (les plus vus, les plus ratés…)
 * @param segments      répartition des comptes par profil d'usage
 * @param recommendations ce qu'il y a à faire, le plus grave d'abord
 */
public record AdminAnalytics(Instant generatedAt, int windowDays, long boxesPublished, long coursesPublished,
                             long sectionsPublished, AudienceMetrics audience, EngagementMetrics engagement,
                             RevenueMetrics revenue, WindowComparison previous, List<UsageSegment> segments,
                             List<ContentInsight> insights, List<Recommendation> recommendations) {
}
