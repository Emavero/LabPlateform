package com.labplatform.adapter.out.hypervisor.docker;

import com.labplatform.adapter.out.process.CommandRunner;
import com.labplatform.domain.lab.AccessProtocol;
import com.labplatform.domain.lab.ConnectionInfo;
import com.labplatform.domain.lab.VirtualMachine;
import com.labplatform.domain.shared.ConflictException;
import com.labplatform.domain.shared.ServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Machines Linux de lab réalisées par des conteneurs Docker exposant SSH.
 * <p>
 * Démarrer = créer un conteneur neuf depuis l'image de lab, publier son port 22
 * sur un port libre de l'hôte et y injecter le mot de passe temporaire.
 * Arrêter = supprimer le conteneur : chaque session repart d'une machine propre
 * et les identifiants précédents disparaissent avec lui.
 * <p>
 * Le mot de passe ne transite que par l'entrée standard de chpasswd : il
 * n'apparaît ni dans les arguments de commande, ni dans l'environnement du conteneur.
 */
public class DockerLinuxMachines {

    private static final Logger log = LoggerFactory.getLogger(DockerLinuxMachines.class);
    private static final String READY_MARKER = "/run/lab-ready";
    private static final int CONSOLE_TAIL = 40;

    private final DockerLabSettings settings;
    private final CommandRunner commands;
    /** Démarrages en cours : un second clic simultané ne doit pas recréer le conteneur. */
    private final Set<Long> starting = ConcurrentHashMap.newKeySet();

    public DockerLinuxMachines(DockerLabSettings settings, CommandRunner commands) {
        this.settings = settings;
        this.commands = commands;
    }

    public ConnectionInfo powerOn(VirtualMachine vm, String password) {
        Long id = vm.getId();
        if (!starting.add(id)) {
            throw new ConflictException("Un démarrage est déjà en cours pour cette machine");
        }
        String name = containerName(vm);
        try {
            // Un conteneur resté d'une session précédente (redémarrage du backend...) est remplacé.
            docker("rm", "-f", name);

            CommandRunner.Result run = docker(runArguments(vm, name));
            if (!run.succeeded()) {
                log.error("Échec de création du conteneur {} : {}", name, run.stderr().strip());
                throw unavailable("Impossible de créer la machine Linux. Vérifiez que Docker est accessible "
                        + "et que l'image " + settings.image() + " est construite.");
            }
            try {
                waitUntilReady(name);
                setPassword(name, password);
                int port = publishedSshPort(name);
                ConnectionInfo info = new ConnectionInfo(settings.publicHost(), port, AccessProtocol.SSH,
                        settings.username(), password);
                log.info("[docker] VM {} démarrée dans le conteneur {} : {}", id, name, info);
                return info;
            } catch (RuntimeException e) {
                docker("rm", "-f", name);
                throw e;
            }
        } finally {
            starting.remove(id);
        }
    }

    public void powerOff(VirtualMachine vm) {
        String name = containerName(vm);
        CommandRunner.Result result = docker("rm", "-f", name);
        if (!result.succeeded() && !isMissingContainer(result)) {
            log.error("Échec de suppression du conteneur {} : {}", name, result.stderr().strip());
            throw unavailable("Impossible d'arrêter la machine Linux : Docker ne répond pas.");
        }
        log.info("[docker] VM {} arrêtée, conteneur {} supprimé", vm.getId(), name);
    }

    public List<String> consoleLog(VirtualMachine vm) {
        if (!vm.isRunning()) {
            return List.of("[info] Machine arrêtée — aucun journal disponible.");
        }
        String name = containerName(vm);
        CommandRunner.Result logs = docker("logs", "--tail", String.valueOf(CONSOLE_TAIL), name);
        if (!logs.succeeded()) {
            return List.of(
                    "[erreur] Le conteneur " + name + " est introuvable.",
                    "[conseil] Arrêtez puis redémarrez la machine pour en recréer une.");
        }
        List<String> lines = new ArrayList<>();
        lines.add("[docker] Conteneur " + name + " (image " + settings.image() + ")");
        vm.getConnection().ifPresent(c ->
                lines.add("[ssh] Connexion : ssh " + c.username() + "@" + c.host() + " -p " + c.port()));
        // docker logs renvoie la sortie du conteneur sur stdout et ses erreurs (sshd -e) sur stderr.
        (logs.stdout() + logs.stderr()).lines().filter(l -> !l.isBlank()).forEach(lines::add);
        return lines;
    }

    /** Nom déterministe : un seul conteneur possible par machine. */
    String containerName(VirtualMachine vm) {
        return settings.containerPrefix() + vm.getId();
    }

    private String[] runArguments(VirtualMachine vm, String name) {
        List<String> args = new ArrayList<>(List.of(
                "run", "--detach",
                "--name", name,
                "--hostname", "lab-" + vm.getId(),
                "--label", "labplatform.vm-id=" + vm.getId(),
                "--label", "labplatform.owner-id=" + vm.getOwnerId(),
                "--env", "LAB_USERNAME=" + settings.username(),
                "--memory", settings.memory(),
                "--cpus", settings.cpus(),
                "--pids-limit", String.valueOf(settings.pidsLimit()),
                // Port hôte choisi par Docker : aucune collision entre utilisateurs.
                "--publish", settings.bindAddress() + "::22"));
        if (!settings.network().isEmpty()) {
            args.add("--network");
            args.add(settings.network());
        }
        args.add(settings.image());
        return args.toArray(String[]::new);
    }

    private void waitUntilReady(String name) {
        long deadline = System.nanoTime() + settings.readyTimeout().toNanos();
        do {
            if (docker("exec", name, "test", "-f", READY_MARKER).succeeded()) {
                return;
            }
            sleep();
        } while (System.nanoTime() < deadline);
        throw unavailable("La machine Linux n'a pas démarré dans le délai prévu. Réessayez.");
    }

    private void setPassword(String name, String password) {
        CommandRunner.Result result = run(command("exec", "--interactive", name, "chpasswd"),
                settings.username() + ":" + password + "\n");
        if (!result.succeeded()) {
            log.error("Échec d'initialisation du mot de passe dans {} : {}", name, result.stderr().strip());
            throw unavailable("Impossible de préparer le compte de la machine Linux.");
        }
    }

    private int publishedSshPort(String name) {
        CommandRunner.Result result = docker("port", name, "22/tcp");
        if (result.succeeded()) {
            Integer port = parsePort(result.stdout());
            if (port != null) {
                return port;
            }
        }
        log.error("Port SSH introuvable pour {} : {} {}", name, result.stdout().strip(), result.stderr().strip());
        throw unavailable("Impossible de déterminer le port SSH de la machine Linux.");
    }

    /** Lit « 127.0.0.1:32771 » ou « [::]:32771 » (une ligne par interface) et renvoie le port. */
    static Integer parsePort(String output) {
        return output.lines()
                .map(String::strip)
                .filter(line -> line.lastIndexOf(':') > 0)
                .map(line -> line.substring(line.lastIndexOf(':') + 1))
                .filter(port -> port.matches("\\d{1,5}"))
                .map(Integer::valueOf)
                .filter(port -> port >= 1 && port <= 65_535)
                .findFirst()
                .orElse(null);
    }

    private static boolean isMissingContainer(CommandRunner.Result result) {
        return result.stderr().toLowerCase().contains("no such container");
    }

    private CommandRunner.Result docker(String... args) {
        return run(command(args), null);
    }

    private CommandRunner.Result run(List<String> command, String stdin) {
        return commands.run(command, stdin, settings.commandTimeout());
    }

    private List<String> command(String... args) {
        List<String> command = new ArrayList<>(args.length + 1);
        command.add(settings.dockerBinary());
        command.addAll(Arrays.asList(args));
        return command;
    }

    private void sleep() {
        try {
            Thread.sleep(Math.max(1, settings.pollInterval().toMillis()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw unavailable("Démarrage interrompu.");
        }
    }

    private static ServiceUnavailableException unavailable(String message) {
        return new ServiceUnavailableException(message);
    }
}
