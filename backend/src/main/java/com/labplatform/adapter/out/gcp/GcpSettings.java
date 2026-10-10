package com.labplatform.adapter.out.gcp;

import com.labplatform.domain.shared.InvalidInputException;

import java.time.Duration;
import java.util.Objects;

/**
 * Coordonnées du projet Compute Engine, lues de la configuration.
 * <p>
 * Rien n'est codé en dur, et rien ne vient du client : le projet, la zone et le
 * nom de l'instance sont des variables d'environnement, et les identifiants du
 * compte de service ne quittent jamais le serveur.
 *
 * @param projectId      identifiant du projet GCP
 * @param zone           zone de l'instance (ex. europe-west1-b)
 * @param instanceName   instance pilotée par le bouton « Démarrer / Arrêter »
 * @param targetTemplate gabarit du nom d'instance d'une cible du catalogue, où
 *                       {@code {slug}} est remplacé par l'identifiant de la
 *                       machine. Même procédé que l'image Docker d'une cible :
 *                       quelle instance héberge quelle machine est un détail de
 *                       déploiement, pas une donnée du catalogue
 * @param operationTimeout attente maximale d'un ordre avant d'abandonner
 * @param perUserTargets   vrai quand chaque apprenant obtient sa propre
 *                         instance, créée au démarrage et détruite à l'arrêt ;
 *                         faux quand une instance préexistante est partagée
 * @param targetBlueprint  paramètres de création d'une cible ; nul en mode
 *                         partagé, où rien n'est créé
 */
public record GcpSettings(String projectId, String zone, String instanceName, String targetTemplate,
                          Duration operationTimeout, boolean perUserTargets, TargetBlueprint targetBlueprint) {

    public GcpSettings {
        Objects.requireNonNull(operationTimeout, "operationTimeout");
        projectId = required(projectId, "GCP_PROJECT_ID");
        zone = required(zone, "GCP_ZONE");
        instanceName = required(instanceName, "GCP_INSTANCE_NAME");
        targetTemplate = targetTemplate == null || targetTemplate.isBlank() ? "{slug}" : targetTemplate.trim();
        if (perUserTargets) {
            // Sans {user}, tous les apprenants viseraient le même nom : le mode
            // dirait « une instance par apprenant » et en partagerait une. Le
            // dire au démarrage vaut mieux que de le découvrir à l'usage.
            if (!targetTemplate.contains("{user}")) {
                throw new InvalidInputException("Configuration GCP incohérente : en mode « per-user », "
                        + "APP_GCP_TARGET_NAME doit contenir {user} (ex. target-{slug}-{user})");
            }
            Objects.requireNonNull(targetBlueprint, "targetBlueprint");
        }
    }

    /** Instance partagée qui héberge une cible du catalogue. */
    public String targetInstanceName(String boxSlug) {
        return targetTemplate.replace("{slug}", boxSlug);
    }

    /** Instance de la cible d'un apprenant, en mode « une instance par apprenant ». */
    public String targetInstanceName(String boxSlug, Long userId) {
        return InstanceName.forTarget(targetTemplate, boxSlug, userId);
    }

    /** Gabarit de création de la cible d'une machine donnée. */
    public TargetBlueprint blueprintFor(String boxSlug) {
        return targetBlueprint.forBox(boxSlug);
    }

    /**
     * Un paramètre manquant est signalé à la configuration, pas plus tard à
     * l'exécution : une plateforme en mode gcp sans projet ne démarrera jamais
     * rien, autant le dire tout de suite.
     */
    private static String required(String value, String variable) {
        if (value == null || value.isBlank()) {
            throw new InvalidInputException("Configuration GCP incomplète : " + variable + " n'est pas défini");
        }
        return value.trim();
    }
}
