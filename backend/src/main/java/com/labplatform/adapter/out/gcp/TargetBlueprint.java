package com.labplatform.adapter.out.gcp;

import com.labplatform.domain.shared.InvalidInputException;

import java.util.List;
import java.util.Objects;

/**
 * Ce qu'il faut savoir pour créer l'instance d'une cible, par opposition à
 * l'allumer : une instance qui préexiste n'a besoin que de son nom.
 * <p>
 * Le modèle « une instance par apprenant » demande de créer la machine à la
 * demande et de la détruire ensuite. Ces paramètres-là ne viennent ni du
 * catalogue ni du client : ce sont des détails de déploiement, lus de la
 * configuration, exactement comme le nom de l'image d'une cible Docker.
 * <p>
 * Une cible <strong>n'a jamais d'adresse externe</strong>. Ce n'est pas un
 * réglage : c'est tout le mécanisme. On ne joint une cible qu'en entrant dans
 * le réseau du lab par le VPN, et lui donner une adresse publique exposerait à
 * Internet une machine volontairement vulnérable. L'adaptateur ne demande donc
 * aucune configuration d'accès, et il n'y a pas de variable pour en ajouter.
 *
 * @param machineType  type de machine (ex. e2-small) ; une cible n'a pas besoin
 *                     d'être grande, elle a besoin d'exister
 * @param sourceImage  image disque de la cible, chemin complet attendu par
 *                     Compute Engine (ex.
 *                     {@code projects/mon-projet/global/images/box-sentinel})
 * @param diskSizeGb   taille du disque de démarrage
 * @param diskType     type de disque (ex. pd-standard)
 * @param subnetwork   sous-réseau où placer la carte réseau ; vide pour celui
 *                     que le projet désigne par défaut dans la région
 * @param networkTags  étiquettes réseau, par lesquelles les règles de pare-feu
 *                     désignent la machine
 */
public record TargetBlueprint(String machineType, String sourceImage, int diskSizeGb, String diskType,
                              String subnetwork, List<String> networkTags) {

    public TargetBlueprint {
        machineType = required(machineType, "APP_GCP_TARGET_MACHINE_TYPE");
        sourceImage = required(sourceImage, "APP_GCP_TARGET_IMAGE");
        diskType = required(diskType, "APP_GCP_TARGET_DISK_TYPE");
        if (diskSizeGb <= 0) {
            throw new InvalidInputException(
                    "Configuration GCP incomplète : APP_GCP_TARGET_DISK_SIZE doit être un nombre de Go positif");
        }
        subnetwork = subnetwork == null ? "" : subnetwork.trim();
        // Une liste venue d'une variable vide contient parfois une chaîne vide ;
        // Compute Engine refuserait l'étiquette, et le message ne dirait pas
        // d'où elle vient.
        networkTags = Objects.requireNonNullElse(networkTags, List.<String>of()).stream()
                .filter(tag -> tag != null && !tag.isBlank())
                .map(String::trim)
                .toList();
    }

    /** Le gabarit dont l'image dépend de la machine du catalogue. */
    public TargetBlueprint forBox(String boxSlug) {
        return new TargetBlueprint(machineType, sourceImage.replace("{slug}", boxSlug), diskSizeGb, diskType,
                subnetwork, networkTags);
    }

    private static String required(String value, String variable) {
        if (value == null || value.isBlank()) {
            throw new InvalidInputException("Configuration GCP incomplète : " + variable + " n'est pas défini");
        }
        return value.trim();
    }
}
