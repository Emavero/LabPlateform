package com.labplatform.application.port.in.vpn;

import java.time.Instant;

/**
 * Ce que l'administration voit du profil déposé : de quoi reconnaître le
 * fichier, sans son contenu — qui ne lui apprendrait rien et contient des clés.
 *
 * @param routesLabNetwork le profil route-t-il le réseau des machines ? Sinon
 *                         le tunnel montera sans joindre les cibles, et il vaut
 *                         mieux le dire au dépôt qu'après la première plainte
 */
public record LabVpnProfileSummary(String fileName, int sizeBytes, Instant uploadedAt, boolean routesLabNetwork) {
}
