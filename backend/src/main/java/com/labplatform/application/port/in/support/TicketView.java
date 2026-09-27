package com.labplatform.application.port.in.support;

import com.labplatform.domain.support.Ticket;

/**
 * Demande telle qu'elle est présentée à un lecteur.
 *
 * @param handle pseudonyme du demandeur : l'équipe voit qui écrit, jamais son
 *               adresse e-mail, qui ne sert à rien pour répondre dans le fil
 * @param mine   vrai si le lecteur en est le demandeur
 */
public record TicketView(Ticket ticket, String handle, boolean mine) {
}
