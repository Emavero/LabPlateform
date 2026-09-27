package com.labplatform.application.port.in.support;

import java.util.List;

/**
 * File d'attente de l'assistance, telle que l'administration la consulte.
 *
 * @param waiting      demandes qui attendent l'équipe, les plus anciennes d'abord :
 *                     c'est l'ordre dans lequel on les traite, pas l'inverse
 * @param answered     demandes où l'équipe a répondu et attend le demandeur
 * @param resolved     demandes closes, les plus récentes d'abord
 * @param byCategory   nombre de demandes par catégorie, toutes demandes confondues :
 *                     dit où la plateforme coince
 * @param medianMinutesToFirstReply délai médian de première réponse, en minutes,
 *                     ou null tant qu'aucune demande n'a reçu de réponse
 */
public record SupportQueue(List<TicketView> waiting, List<TicketView> answered, List<TicketView> resolved,
                           List<CategoryTally> byCategory, Long medianMinutesToFirstReply) {

    public record CategoryTally(String category, String categoryName, long count) {
    }
}
