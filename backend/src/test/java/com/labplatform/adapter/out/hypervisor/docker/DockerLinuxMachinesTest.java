package com.labplatform.adapter.out.hypervisor.docker;

import com.labplatform.adapter.out.process.CommandRunner;
import com.labplatform.domain.lab.AccessProtocol;
import com.labplatform.domain.lab.ConnectionInfo;
import com.labplatform.domain.lab.OperatingSystem;
import com.labplatform.domain.lab.VirtualMachine;
import com.labplatform.domain.lab.VmStatus;
import com.labplatform.domain.shared.ServiceUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerLinuxMachinesTest {

    private static final String PASSWORD = "Secr3tTempPass99";
    private static final String CONTAINER = "labplatform-vm-7";

    private FakeDocker docker;
    private DockerLinuxMachines machines;
    private VirtualMachine stopped;

    @BeforeEach
    void setUp() {
        docker = new FakeDocker();
        machines = new DockerLinuxMachines(settings(Duration.ofMillis(50)), docker);
        stopped = VirtualMachine.restore(7L, 3L, OperatingSystem.LINUX, VmStatus.STOPPED, null, null);
    }

    @Test
    void demarrerCreeUnConteneurEtRenvoieLesAccesSsh() {
        ConnectionInfo info = machines.powerOn(stopped, PASSWORD);

        assertEquals("localhost", info.host());
        assertEquals(32771, info.port());
        assertEquals(AccessProtocol.SSH, info.protocol());
        assertEquals("labuser", info.username());
        assertEquals(PASSWORD, info.password());

        List<String> run = docker.call("run");
        assertTrue(run.contains("--publish") && run.contains("127.0.0.1::22"), "port SSH publié sur un port libre");
        assertTrue(run.contains("LAB_USERNAME=labuser"));
        assertEquals("labplatform/lab-linux:latest", run.get(run.size() - 1));
        assertEquals("labuser:" + PASSWORD + "\n", docker.stdinOf("chpasswd"));
    }

    @Test
    void leMotDePasseNapparaitDansAucunArgumentDeCommande() {
        machines.powerOn(stopped, PASSWORD);

        for (List<String> call : docker.calls) {
            assertFalse(String.join(" ", call).contains(PASSWORD), "mot de passe visible dans : " + call);
        }
    }

    @Test
    void unConteneurRestantDUneSessionPrecedenteEstRemplace() {
        machines.powerOn(stopped, PASSWORD);

        assertEquals(List.of("docker", "rm", "-f", CONTAINER), docker.calls.get(0));
    }

    @Test
    void echecDeCreationSignaleUneIndisponibiliteSansInjecterDeMotDePasse() {
        docker.fail("run", "Unable to find image");

        assertThrows(ServiceUnavailableException.class, () -> machines.powerOn(stopped, PASSWORD));
        assertTrue(docker.call("chpasswd") == null);
    }

    @Test
    void machineJamaisPreteEstSupprimee() {
        docker.fail("test", "");

        assertThrows(ServiceUnavailableException.class, () -> machines.powerOn(stopped, PASSWORD));
        List<String> last = docker.calls.get(docker.calls.size() - 1);
        assertEquals(List.of("docker", "rm", "-f", CONTAINER), last);
    }

    @Test
    void arreterSupprimeLeConteneurMemeSilEstDejaAbsent() {
        docker.fail("rm", "Error response from daemon: No such container: " + CONTAINER);

        machines.powerOff(running());

        assertEquals(List.of("docker", "rm", "-f", CONTAINER), docker.calls.get(0));
    }

    @Test
    void arreterEchoueSiDockerNeRepondPas() {
        docker.fail("rm", "Cannot connect to the Docker daemon");

        assertThrows(ServiceUnavailableException.class, () -> machines.powerOff(running()));
    }

    @Test
    void journalReprendLaSortieDuConteneur() {
        List<String> log = machines.consoleLog(running());

        assertTrue(log.contains("[ssh] Connexion : ssh labuser@localhost -p 32771"));
        assertTrue(log.contains("Server listening on 0.0.0.0 port 22."));
    }

    @Test
    void lectureDuPortPublie() {
        assertEquals(Integer.valueOf(32771), DockerLinuxMachines.parsePort("127.0.0.1:32771\n"));
        assertEquals(Integer.valueOf(40022), DockerLinuxMachines.parsePort("0.0.0.0:40022\n[::]:40022\n"));
        assertEquals(null, DockerLinuxMachines.parsePort(""));
    }

    @Test
    void nomDUtilisateurDangereuxRefuseDesLaConfiguration() {
        assertThrows(IllegalArgumentException.class, () -> new DockerLabSettings("docker", "img", "localhost",
                "127.0.0.1", "", "512m", "1", 256, "labplatform-vm-", "root; rm -rf /",
                Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofMillis(1)));
    }

    private VirtualMachine running() {
        ConnectionInfo info = new ConnectionInfo("localhost", 32771, AccessProtocol.SSH, "labuser", PASSWORD);
        return VirtualMachine.restore(7L, 3L, OperatingSystem.LINUX, VmStatus.RUNNING, info, Instant.EPOCH);
    }

    private static DockerLabSettings settings(Duration readyTimeout) {
        return new DockerLabSettings("docker", "labplatform/lab-linux:latest", "localhost", "127.0.0.1", "",
                "512m", "1.0", 256, "labplatform-vm-", "labuser",
                Duration.ofSeconds(5), readyTimeout, Duration.ofMillis(1));
    }

    /** Faux client Docker : répond selon la sous-commande et garde la trace des appels. */
    private static final class FakeDocker implements CommandRunner {
        final List<List<String>> calls = new ArrayList<>();
        final List<String> stdins = new ArrayList<>();
        private final java.util.Map<String, Function<List<String>, Result>> overrides = new java.util.HashMap<>();

        void fail(String keyword, String stderr) {
            overrides.put(keyword, cmd -> new Result(1, "", stderr));
        }

        List<String> call(String keyword) {
            return calls.stream().filter(c -> c.contains(keyword)).findFirst().orElse(null);
        }

        String stdinOf(String keyword) {
            for (int i = 0; i < calls.size(); i++) {
                if (calls.get(i).contains(keyword)) {
                    return stdins.get(i);
                }
            }
            return null;
        }

        @Override
        public Result run(List<String> command, String stdin, Duration timeout) {
            calls.add(List.copyOf(command));
            stdins.add(stdin);
            for (var entry : overrides.entrySet()) {
                if (command.contains(entry.getKey())) {
                    return entry.getValue().apply(command);
                }
            }
            return switch (command.get(1)) {
                case "port" -> new Result(0, "127.0.0.1:32771\n", "");
                case "logs" -> new Result(0, "[lab] Compte labuser créé\n", "Server listening on 0.0.0.0 port 22.\n");
                default -> new Result(0, "", "");
            };
        }
    }
}
