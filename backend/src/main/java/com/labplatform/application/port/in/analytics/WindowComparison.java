package com.labplatform.application.port.in.analytics;

/**
 * Les mêmes chiffres sur la période <em>précédente</em>, de même longueur.
 * <p>
 * Un indicateur sans comparaison ne dit rien : « 42 flags validés » n'est bon
 * ou mauvais qu'au regard des 12 du mois d'avant. C'est cette comparaison qui
 * transforme un chiffre en tendance, et elle n'est calculée que pour les
 * quelques indicateurs qui mènent la lecture.
 */
public record WindowComparison(long newUsers, long activeUsers, long flagsValidated, long sectionsCompleted,
                               long subscriptionsStarted) {
}
