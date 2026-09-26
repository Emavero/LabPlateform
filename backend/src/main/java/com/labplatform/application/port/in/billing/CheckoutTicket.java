package com.labplatform.application.port.in.billing;

/**
 * Où envoyer le payeur, et sous quelle référence retrouver son paiement au
 * retour. L'identifiant du prestataire ne sort pas d'ici : le client n'a
 * besoin que de notre référence.
 */
public record CheckoutTicket(String reference, String redirectUrl) {
}
