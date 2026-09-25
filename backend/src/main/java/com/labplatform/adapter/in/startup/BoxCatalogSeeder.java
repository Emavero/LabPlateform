package com.labplatform.adapter.in.startup;

import com.labplatform.application.port.out.BoxRepositoryPort;
import com.labplatform.application.port.out.SecretGeneratorPort;
import com.labplatform.config.AppProperties;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.Difficulty;
import com.labplatform.domain.box.Flag;
import com.labplatform.domain.lab.OperatingSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Sème le catalogue au premier démarrage, une seule fois : si la table
 * contient déjà des machines, rien n'est touché.
 * <p>
 * Les flags sont tirés au hasard à ce moment-là et seule leur empreinte est
 * conservée. Sur une vraie infrastructure, ils sont déposés sur la machine
 * cible à sa construction ; ici les machines sont simulées, donc le mode
 * démonstration ({@code app.boxes.log-seeded-flags=true}, comme
 * {@code app.security.expose-reset-token-in-response}) les journalise une
 * fois au démarrage pour pouvoir essayer le parcours de bout en bout.
 */
@Component
public class BoxCatalogSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BoxCatalogSeeder.class);

    /**
     * Catalogue par défaut. Ajouter une machine revient à ajouter une ligne :
     * le barème découle de la difficulté, aucun point n'est écrit ici.
     */
    private static final List<Blueprint> CATALOGUE = List.of(
            new Blueprint("sentinel", "Sentinel", OperatingSystem.LINUX, Difficulty.VERY_EASY, "10.10.10.11",
                    "Un serveur web de démonstration laissé en place après une recette. Énumération, "
                            + "identifiants par défaut et une tâche planifiée trop permissive.", 60),
            new Blueprint("northwind", "Northwind", OperatingSystem.WINDOWS, Difficulty.EASY, "10.10.10.12",
                    "Partage SMB ouvert en lecture sur un contrôleur de domaine de test. Le chemin vers "
                            + "l'administration passe par une délégation mal configurée.", 48),
            new Blueprint("cobalt", "Cobalt", OperatingSystem.LINUX, Difficulty.EASY, "10.10.10.13",
                    "API REST exposant un point d'entrée de téléversement. Le conteneur qui l'héberge "
                            + "monte le socket de l'hôte.", 35),
            new Blueprint("mirage", "Mirage", OperatingSystem.LINUX, Difficulty.MEDIUM, "10.10.10.14",
                    "Portail interne derrière un proxy inverse. Une injection de modèle côté serveur "
                            + "ouvre la première porte ; la seconde est une binaire setuid maison.", 21),
            new Blueprint("blackice", "BlackIce", OperatingSystem.WINDOWS, Difficulty.HARD, "10.10.10.15",
                    "Serveur applicatif d'une chaîne de production. Désérialisation .NET, puis "
                            + "récupération de tickets Kerberos pour rebondir vers l'administration.", 14),
            new Blueprint("obsidian", "Obsidian", OperatingSystem.LINUX, Difficulty.INSANE, "10.10.10.16",
                    "Machine de fin de parcours : évasion d'un bac à sable applicatif, chaîne de "
                            + "rebonds sur le réseau interne et élévation par un pilote noyau vulnérable.", 7));

    private final BoxRepositoryPort boxes;
    private final SecretGeneratorPort secrets;
    private final AppProperties properties;
    private final Clock clock;

    public BoxCatalogSeeder(BoxRepositoryPort boxes, SecretGeneratorPort secrets, AppProperties properties,
                            Clock clock) {
        this.boxes = boxes;
        this.secrets = secrets;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (boxes.count() > 0) {
            return;
        }
        Instant now = clock.instant();
        CATALOGUE.forEach(blueprint -> seed(blueprint, now));
        log.info("Catalogue initialisé : {} machines", CATALOGUE.size());
    }

    private void seed(Blueprint blueprint, Instant now) {
        String userSecret = secrets.hexToken();
        String rootSecret = secrets.hexToken();
        boxes.save(Box.create(blueprint.slug(), blueprint.name(), blueprint.operatingSystem(),
                blueprint.difficulty(), blueprint.synopsis(), blueprint.ipAddress(), "cyberMans",
                now.minus(Duration.ofDays(blueprint.releasedDaysAgo())),
                Flag.ofSecret(userSecret), Flag.ofSecret(rootSecret)));

        if (properties.getBoxes().isLogSeededFlags()) {
            log.info("[démo] {} — flag user {} / flag root {}", blueprint.slug(), userSecret, rootSecret);
        }
    }

    private record Blueprint(String slug, String name, OperatingSystem operatingSystem, Difficulty difficulty,
                             String ipAddress, String synopsis, int releasedDaysAgo) {
    }
}
