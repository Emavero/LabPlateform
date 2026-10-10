package com.labplatform.adapter.out.hypervisor.gcp;

import com.labplatform.adapter.out.gcp.ComputeInstances;
import com.labplatform.adapter.out.gcp.GcpSettings;
import com.labplatform.adapter.out.gcp.TargetBlueprint;
import com.labplatform.config.AppProperties;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.Difficulty;
import com.labplatform.domain.box.Flag;
import com.labplatform.domain.lab.MachineState;
import com.labplatform.domain.lab.OperatingSystem;
import com.labplatform.domain.lab.VmStatus;
import com.labplatform.domain.shared.ConflictException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Les deux modèles de cible, côte à côte.
 * <p>
 * En mode partagé, une instance préexiste et s'allume. En mode par apprenant,
 * elle est créée puis détruite, et surtout : deux apprenants ne touchent jamais
 * la même. C'est cette dernière garantie qui distingue le modèle de
 * HackTheBox de ce que la plateforme faisait avant.
 */
class GcpHypervisorAdapterTest {

    private static final Instant NOW = Instant.parse("2026-10-10T09:00:00Z");

    /** Instances dont on fixe l'état, et qui notent les ordres reçus. */
    private static final class FakeInstances implements ComputeInstances {

        private final Map<String, MachineState> states = new HashMap<>();
        private final List<String> orders = new ArrayList<>();

        @Override
        public MachineState describe(String instanceName) {
            return states.getOrDefault(instanceName, MachineState.of(VmStatus.TERMINATED));
        }

        @Override
        public Optional<MachineState> find(String instanceName) {
            return Optional.ofNullable(states.get(instanceName));
        }

        @Override
        public MachineState start(String instanceName) {
            orders.add("start:" + instanceName);
            MachineState running = MachineState.running("10.20.0.5");
            states.put(instanceName, running);
            return running;
        }

        @Override
        public MachineState stop(String instanceName) {
            orders.add("stop:" + instanceName);
            MachineState stopped = MachineState.of(VmStatus.TERMINATED);
            states.put(instanceName, stopped);
            return stopped;
        }

        @Override
        public MachineState create(String instanceName, TargetBlueprint blueprint) {
            orders.add("create:" + instanceName + ":" + blueprint.sourceImage());
            MachineState running = MachineState.running("10.20.0." + (states.size() + 10));
            states.put(instanceName, running);
            return running;
        }

        @Override
        public void delete(String instanceName) {
            orders.add("delete:" + instanceName);
            states.remove(instanceName);
        }
    }

    // ------------------------------------------------- Une instance par apprenant

    @Test
    void eachLearnerGetsAMachineOfTheirOwn() {
        FakeInstances instances = new FakeInstances();
        GcpHypervisorAdapter adapter = perUser(instances);
        Box box = box("sentinel");

        String first = adapter.powerOnTarget(box, 1L);
        String second = adapter.powerOnTarget(box, 2L);

        assertTrue(instances.orders.contains("create:target-sentinel-1:images/box-sentinel"));
        assertTrue(instances.orders.contains("create:target-sentinel-2:images/box-sentinel"));
        // Deux instances, donc deux adresses : l'apprenant qui voit celle de son
        // voisin croirait attaquer la même machine.
        assertNotEquals(first, second);
    }

    @Test
    void stoppingDestroysOnlyTheLearnersOwnMachine() {
        FakeInstances instances = new FakeInstances();
        GcpHypervisorAdapter adapter = perUser(instances);
        Box box = box("sentinel");
        adapter.powerOnTarget(box, 1L);
        adapter.powerOnTarget(box, 2L);

        adapter.powerOffTarget(box, 1L);

        assertTrue(instances.orders.contains("delete:target-sentinel-1"));
        assertFalse(instances.orders.contains("delete:target-sentinel-2"));
        // Celle du second tourne toujours : c'est tout l'intérêt du modèle.
        assertTrue(instances.find("target-sentinel-2").orElseThrow().status().isRunning());
    }

    @Test
    void theMachineIsDestroyedRatherThanSwitchedOff() {
        // Une instance éteinte continue de facturer son disque. Détruire est le
        // choix de HackTheBox, et le seul qui ne coûte rien entre deux séances.
        FakeInstances instances = new FakeInstances();
        GcpHypervisorAdapter adapter = perUser(instances);
        Box box = box("sentinel");
        adapter.powerOnTarget(box, 1L);

        adapter.powerOffTarget(box, 1L);

        assertTrue(instances.orders.contains("delete:target-sentinel-1"));
        assertFalse(instances.orders.contains("stop:target-sentinel-1"));
        assertTrue(instances.find("target-sentinel-1").isEmpty());
    }

    @Test
    void askingTwiceDoesNotRecreateAMachineAlreadyRunning() {
        // Un double clic, un rafraîchissement : la même demande arrive deux
        // fois. Détruire pour recréer ferait perdre le travail en cours.
        FakeInstances instances = new FakeInstances();
        GcpHypervisorAdapter adapter = perUser(instances);
        Box box = box("sentinel");

        String first = adapter.powerOnTarget(box, 1L);
        String again = adapter.powerOnTarget(box, 1L);

        assertEquals(first, again);
        assertEquals(1, instances.orders.stream().filter(o -> o.startsWith("create:")).count());
    }

    @Test
    void anInstanceLeftSwitchedOffIsRelitRatherThanRebuilt() {
        // Un arrêt précédent n'a pas pu détruire la machine : la rallumer coûte
        // moins qu'une recréation, et l'apprenant retrouve son état.
        FakeInstances instances = new FakeInstances();
        instances.states.put("target-sentinel-1", MachineState.of(VmStatus.TERMINATED));
        GcpHypervisorAdapter adapter = perUser(instances);

        adapter.powerOnTarget(box("sentinel"), 1L);

        assertTrue(instances.orders.contains("start:target-sentinel-1"));
        assertFalse(instances.orders.stream().anyMatch(o -> o.startsWith("create:")));
    }

    @Test
    void aMachineInTransitionIsNotTouched() {
        FakeInstances instances = new FakeInstances();
        instances.states.put("target-sentinel-1", MachineState.of(VmStatus.STAGING));
        GcpHypervisorAdapter adapter = perUser(instances);

        assertThrows(ConflictException.class, () -> adapter.powerOnTarget(box("sentinel"), 1L));
        assertTrue(instances.orders.isEmpty());
    }

    // ------------------------------------------------------- Instance partagée

    @Test
    void theSharedModelStillStartsThePreexistingInstance() {
        FakeInstances instances = new FakeInstances();
        GcpHypervisorAdapter adapter = shared(instances);

        adapter.powerOnTarget(box("sentinel"), 1L);
        adapter.powerOffTarget(box("sentinel"), 1L);

        // Ni création ni destruction : l'instance appartient à l'exploitant.
        assertEquals(List.of("start:target-sentinel", "stop:target-sentinel"), instances.orders);
    }

    @Test
    void theSharedModelIgnoresWhoAsks() {
        FakeInstances instances = new FakeInstances();
        GcpHypervisorAdapter adapter = shared(instances);
        Box box = box("sentinel");

        assertEquals(adapter.powerOnTarget(box, 1L), adapter.powerOnTarget(box, 2L));
    }

    // ------------------------------------------------------------------ Outils

    private static GcpHypervisorAdapter perUser(ComputeInstances instances) {
        TargetBlueprint blueprint = new TargetBlueprint("e2-small", "images/box-{slug}", 20, "pd-standard",
                "", List.of("lab-target"));
        GcpSettings settings = new GcpSettings("projet", "europe-west1-b", "cible-unique", "target-{slug}-{user}",
                Duration.ofSeconds(60), true, blueprint);
        return new GcpHypervisorAdapter(instances, settings, new AppProperties());
    }

    private static GcpHypervisorAdapter shared(ComputeInstances instances) {
        GcpSettings settings = new GcpSettings("projet", "europe-west1-b", "cible-unique", "target-{slug}",
                Duration.ofSeconds(60), false, null);
        return new GcpHypervisorAdapter(instances, settings, new AppProperties());
    }

    private static Box box(String slug) {
        return Box.create(slug, slug, OperatingSystem.LINUX, Difficulty.EASY, "Synopsis.", "10.10.10.11",
                "cyberMans", NOW, Flag.ofSecret("11111111111111111111111111111111"),
                Flag.ofSecret("22222222222222222222222222222222"));
    }
}
