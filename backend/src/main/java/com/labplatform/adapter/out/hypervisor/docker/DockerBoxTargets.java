package com.labplatform.adapter.out.hypervisor.docker;

import com.labplatform.adapter.out.process.CommandRunner;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.shared.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Cibles du catalogue réalisées par des conteneurs Docker.
 * <p>
 * Une cible est lancée depuis l'image de sa machine, nommée d'après son
 * identifiant d'URL (« labplatform/box-sentinel:latest » par défaut). Elle
 * n'expose aucun port sur l'hôte : on la joint par son adresse dans le réseau
 * du lab, une fois le VPN monté. C'est la différence avec une machine
 * d'attaque, publiée en SSH sur un port de l'hôte.
 * <p>
 * Chaque joueur obtient son propre conteneur, donc sa propre instance de la
 * cible : ce qu'il y casse ne dérange personne.
 */
public class DockerBoxTargets {

    private static final Logger log = LoggerFactory.getLogger(DockerBoxTargets.class);

    private final DockerLabSettings settings;
    private final String imagePattern;
    private final CommandRunner commands;

    public DockerBoxTargets(DockerLabSettings settings, String imagePattern, CommandRunner commands) {
        this.settings = settings;
        this.imagePattern = imagePattern;
        this.commands = commands;
    }

    /** Lance la cible et renvoie son adresse dans le réseau du lab. */
    public String powerOn(Box box, Long userId) {
        String name = containerName(box, userId);
        String image = imageOf(box);

        // Un conteneur resté d'une session précédente est remplacé : la cible repart propre.
        docker("rm", "-f", name);

        CommandRunner.Result run = docker(runArguments(name, image));
        if (!run.succeeded()) {
            log.error("Échec de lancement de la cible {} : {}", name, run.stderr().strip());
            throw new ServiceUnavailableException("Impossible de lancer cette machine. Vérifiez que l'image "
                    + image + " existe sur le serveur.");
        }
        String address = inspectAddress(name);
        if (address.isBlank()) {
            docker("rm", "-f", name);
            throw new ServiceUnavailableException("La machine a démarré sans adresse réseau exploitable.");
        }
        log.info("[docker] cible {} lancée pour l'utilisateur {} : {}", box.getSlug(), userId, address);
        return address;
    }

    public void powerOff(Box box, Long userId) {
        String name = containerName(box, userId);
        CommandRunner.Result removed = docker("rm", "-f", name);
        if (removed.succeeded()) {
            log.info("[docker] cible {} arrêtée pour l'utilisateur {}", box.getSlug(), userId);
        } else {
            // L'instance est de toute façon considérée comme arrêtée côté application.
            log.warn("Échec de suppression du conteneur {} : {}", name, removed.stderr().strip());
        }
    }

    private List<String> runArguments(String name, String image) {
        List<String> arguments = new ArrayList<>(List.of(
                "run", "--detach", "--name", name,
                "--label", "labplatform.box-target=true",
                "--memory", settings.memory(),
                "--cpus", settings.cpus(),
                "--pids-limit", String.valueOf(settings.pidsLimit()),
                // Une cible est faite pour être attaquée : elle reste confinée.
                "--cap-drop", "NET_RAW",
                "--security-opt", "no-new-privileges"));
        if (!settings.network().isBlank()) {
            arguments.addAll(List.of("--network", settings.network()));
        }
        arguments.add(image);
        return arguments;
    }

    private String inspectAddress(String name) {
        CommandRunner.Result result = docker("inspect", "--format",
                "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}", name);
        return result.succeeded() ? result.stdout().strip() : "";
    }

    private String imageOf(Box box) {
        return imagePattern.replace("{slug}", box.getSlug());
    }

    private String containerName(Box box, Long userId) {
        return settings.containerPrefix() + "target-" + box.getSlug() + "-" + userId;
    }

    private CommandRunner.Result docker(String... arguments) {
        return docker(List.of(arguments));
    }

    private CommandRunner.Result docker(List<String> arguments) {
        List<String> command = new ArrayList<>(arguments.size() + 1);
        command.add(settings.dockerBinary());
        command.addAll(arguments);
        return commands.run(command, null, settings.commandTimeout());
    }
}
