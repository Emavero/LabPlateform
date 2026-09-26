package com.labplatform.application.port.in.writeup;

import com.labplatform.domain.writeup.Writeup;

/**
 * Compte rendu tel qu'il est présenté à un lecteur.
 *
 * @param handle pseudonyme de l'auteur : jamais son adresse e-mail
 * @param mine   vrai si le lecteur en est l'auteur
 */
public record WriteupView(Writeup writeup, String handle, boolean mine) {
}
