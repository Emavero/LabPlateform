package com.labplatform.adapter.out.machine;

import com.labplatform.adapter.out.gcp.ComputeInstances;
import com.labplatform.domain.lab.MachineState;
import com.labplatform.domain.lab.VmStatus;
import com.labplatform.domain.shared.ConflictException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Le pilotage de la cible partagée, sans projet GCP : la couture
 * {@link ComputeInstances} est remplacée par une double, et par la cible
 * simulée qui traverse réellement ses états.
 */
class CloudInstanceAdapterTest {

    private static final String NAME = "target-01";
    private static final Instant NOW = Instant.parse("2026-10-03T09:00:00Z");

    /** Instance dont on fixe l'état, et qui note les ordres reçus. */
    private static class FakeInstances implements ComputeInstances {
        private MachineState state;
        private final List<String> orders = new ArrayList<>();

        FakeInstances(MachineState state) {
            this.state = state;
        }

        @Override
        public MachineState describe(String instanceName) {
            return state;
        }

        @Override
        public MachineState start(String instanceName) {
            orders.add("start " + instanceName);
            state = MachineState.of(VmStatus.STAGING);
            return state;
        }

        @Override
        public MachineState stop(String instanceName) {
            orders.add("stop " + instanceName);
            state = MachineState.of(VmStatus.STOPPING);
            return state;
        }
    }

    @Test
    void startingAStoppedMachineReturnsItsTransition() {
        FakeInstances instances = new FakeInstances(MachineState.of(VmStatus.TERMINATED));

        MachineState state = new CloudInstanceAdapter(instances, NAME).start();

        assertEquals(VmStatus.STAGING, state.status());
        assertTrue(state.status().isTransitional());
        assertEquals(List.of("start " + NAME), instances.orders);
    }

    @Test
    void startingARunningMachineIsRefusedWithoutCallingTheProvider() {
        // Compute Engine accepte cet ordre sans rien faire : le laisser passer
        // afficherait « démarrage » pour une machine déjà prête.
        FakeInstances instances = new FakeInstances(MachineState.running("10.10.10.10"));

        ConflictException refused = assertThrows(ConflictException.class,
                () -> new CloudInstanceAdapter(instances, NAME).start());

        assertEquals("La machine est déjà en cours d'exécution", refused.getMessage());
        assertTrue(instances.orders.isEmpty());
    }

    @Test
    void stoppingAnAlreadyStoppedMachineIsRefused() {
        FakeInstances instances = new FakeInstances(MachineState.of(VmStatus.TERMINATED));

        assertEquals("La machine est déjà arrêtée",
                assertThrows(ConflictException.class, () -> new CloudInstanceAdapter(instances, NAME).stop())
                        .getMessage());
        assertTrue(instances.orders.isEmpty());
    }

    @Test
    void aMachineInTransitionAcceptsNoOrder() {
        for (VmStatus transitional : List.of(VmStatus.PROVISIONING, VmStatus.STAGING, VmStatus.STOPPING)) {
            FakeInstances instances = new FakeInstances(MachineState.of(transitional));
            CloudInstanceAdapter adapter = new CloudInstanceAdapter(instances, NAME);

            assertThrows(ConflictException.class, adapter::start, "démarrage pendant " + transitional);
            assertThrows(ConflictException.class, adapter::stop, "extinction pendant " + transitional);
            assertTrue(instances.orders.isEmpty());
        }
    }

    @Test
    void theInternalAddressIsGivenOnlyWhileRunning() {
        assertEquals("10.10.10.10",
                new CloudInstanceAdapter(new FakeInstances(MachineState.running("10.10.10.10")), NAME)
                        .state().internalIp());
        assertEquals(null,
                new CloudInstanceAdapter(new FakeInstances(MachineState.of(VmStatus.STAGING)), NAME)
                        .state().internalIp());
    }

    // ------------------------------------------------------- Cible simulée

    // --------------------------------- Adresse annoncée à la place de celle du VPC

    @Test
    void theOperatorCanAdvertiseTheTunnelAddressInsteadOfTheProviderOne() {
        // Compute Engine rend l'adresse de la carte réseau dans le VPC. Quand la
        // passerelle VPN et la cible sont la même machine, l'apprenant joint
        // 10.8.0.1 et rien d'autre : montrer 10.128.0.2 donnerait une adresse
        // qui ne répond pas, et un bouton qui semble pourtant marcher.
        FakeInstances instances = new FakeInstances(MachineState.running("10.128.0.2"));

        CloudInstanceAdapter adapter = new CloudInstanceAdapter(instances, NAME, "10.8.0.1");

        assertEquals("10.8.0.1", adapter.state().internalIp());
    }

    @Test
    void withoutAnAdvertisedAddressTheProviderOneIsKept() {
        FakeInstances instances = new FakeInstances(MachineState.running("10.128.0.2"));

        assertEquals("10.128.0.2", new CloudInstanceAdapter(instances, NAME).state().internalIp());
        assertEquals("10.128.0.2", new CloudInstanceAdapter(instances, NAME, "   ").state().internalIp());
    }

    @Test
    void anAdvertisedAddressNeverInventsOneForAMachineThatIsNotRunning() {
        // Un état de passage n'a pas d'adresse : en poser une ferait croire
        // qu'on peut déjà s'y connecter.
        FakeInstances instances = new FakeInstances(MachineState.of(VmStatus.STAGING));

        MachineState state = new CloudInstanceAdapter(instances, NAME, "10.8.0.1").state();

        assertEquals(VmStatus.STAGING, state.status());
        assertNull(state.internalIp());
    }

    @Test
    void theAdvertisedAddressAlsoAppliesRightAfterAnOrder() {
        FakeInstances instances = new FakeInstances(MachineState.of(VmStatus.TERMINATED)) {
            @Override
            public MachineState start(String instanceName) {
                super.start(instanceName);
                return MachineState.running("10.128.0.2");
            }
        };

        assertEquals("10.8.0.1", new CloudInstanceAdapter(instances, NAME, "10.8.0.1").start().internalIp());
    }

    @Test
    void theSimulatedTargetReallyGoesThroughItsTransitions() {
        // Horloge mobile : la transition s'observe sans attendre.
        Instant[] now = { NOW };
        Clock clock = new Clock() {
            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now[0];
            }
        };
        CloudInstanceAdapter adapter = new CloudInstanceAdapter(
                new SimulatedComputeInstances(clock, Duration.ofSeconds(10), Duration.ofSeconds(4), "10.10.10.10"),
                NAME);

        assertEquals(VmStatus.TERMINATED, adapter.state().status());

        assertEquals(VmStatus.STAGING, adapter.start().status());
        // Toujours en démarrage : aucune adresse, et le bouton reste inactif.
        now[0] = NOW.plusSeconds(9);
        assertEquals(VmStatus.STAGING, adapter.state().status());
        assertEquals(null, adapter.state().internalIp());

        now[0] = NOW.plusSeconds(10);
        assertEquals(VmStatus.RUNNING, adapter.state().status());
        assertEquals("10.10.10.10", adapter.state().internalIp());
        assertFalse(adapter.state().status().isTransitional());

        assertEquals(VmStatus.STOPPING, adapter.stop().status());
        now[0] = NOW.plusSeconds(14);
        assertEquals(VmStatus.TERMINATED, adapter.state().status());
    }
}
