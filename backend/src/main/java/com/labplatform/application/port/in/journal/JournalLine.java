package com.labplatform.application.port.in.journal;

import com.labplatform.domain.journal.JournalEvent;

/**
 * Une ligne du journal telle qu'on la lit.
 *
 * @param handle pseudonyme de l'auteur, jamais son adresse e-mail : le journal
 *               d'administration se lit à plusieurs et n'a pas à divulguer les
 *               coordonnées des comptes.
 */
public record JournalLine(JournalEvent event, String handle) {
}
