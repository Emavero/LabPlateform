package com.labplatform.application.service;

import com.labplatform.application.fakes.Fakes;
import com.labplatform.application.fakes.InMemoryBoxes;
import com.labplatform.application.fakes.InMemoryInstances;
import com.labplatform.application.fakes.InMemoryOwns;
import com.labplatform.application.fakes.InMemoryRatings;
import com.labplatform.application.port.in.box.BoxView;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.Difficulty;
import com.labplatform.domain.box.Flag;
import com.labplatform.domain.lab.OperatingSystem;
import com.labplatform.domain.shared.ConflictException;
import com.labplatform.domain.shared.NotFoundException;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BoxInstanceServiceTest {

    private static final Actor ALICE = new Actor(1L, Role.USER);
    private static final Actor BOB = new Actor(2L, Role.USER);
    private static final Instant NOW = Instant.parse("2026-09-26T09:00:00Z");
    private static final Duration LIFETIME = Duration.ofHours(2);

    private InMemoryInstances instances;
    private Fakes.RecordingHypervisor hypervisor;
    private BoxService catalogue;
    private BoxInstanceService spawns;
    /** Horloge mobile : les tests avancent le temps pour éprouver l'échéance. */
    private Instant now;

    @BeforeEach
    void setUp() {
        InMemoryBoxes boxes = new InMemoryBoxes();
        instances = new InMemoryInstances();
        hypervisor = new Fakes.RecordingHypervisor();
        now = NOW;
        Clock clock = new Clock() {
            @Override
            public java.time.ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return now;
            }
        };

        boxes.save(box("sentinel", "10.10.10.11"));
        boxes.save(box("mirage", "10.10.10.14"));
        ScoreboardService scoreboard = new ScoreboardService(boxes, new InMemoryOwns());
        catalogue = new BoxService(boxes, new InMemoryOwns(), new InMemoryRatings(), instances, scoreboard,
                Fakes.NO_TRANSACTION, clock);
        spawns = new BoxInstanceService(boxes, instances, hypervisor, catalogue, Fakes.NO_TRANSACTION, clock,
                LIFETIME);
    }

    private static Box box(String slug, String ip) {
        return Box.create(slug, slug, OperatingSystem.LINUX, Difficulty.EASY, "Synopsis.", ip, "cyberMans", NOW,
                Flag.ofSecret("11111111111111111111111111111111"),
                Flag.ofSecret("22222222222222222222222222222222"));
    }

    @Test
    void spawningACibleGivesItsAddressAndAnExpiry() {
        BoxView view = spawns.spawn(ALICE, "sentinel");

        assertTrue(view.isInstanceRunning());
        assertEquals("10.10.10.11", view.instance().getAddress().orElseThrow());
        assertEquals(NOW.plus(LIFETIME), view.instance().getExpiresAt().orElseThrow());
        assertEquals(LIFETIME, view.instance().remainingAt(NOW));
        assertTrue(hypervisor.calls.contains("target-on:sentinel:1"));
    }

    @Test
    void onlyOneTargetRunsAtATimePerPlayer() {
        spawns.spawn(ALICE, "sentinel");

        ConflictException error = assertThrows(ConflictException.class, () -> spawns.spawn(ALICE, "mirage"));
        assertTrue(error.getMessage().contains("Une autre machine tourne déjà"));
        // Celle d'un autre joueur ne gêne pas.
        assertTrue(spawns.spawn(BOB, "mirage").isInstanceRunning());
    }

    @Test
    void relaunchingTheSameTargetIsRefused() {
        spawns.spawn(ALICE, "sentinel");

        assertThrows(ConflictException.class, () -> spawns.spawn(ALICE, "sentinel"));
    }

    @Test
    void stoppingReleasesThePlayerAndTheInfrastructure() {
        spawns.spawn(ALICE, "sentinel");

        BoxView stopped = spawns.stop(ALICE, "sentinel");

        assertFalse(stopped.isInstanceRunning());
        assertTrue(hypervisor.calls.contains("target-off:sentinel:1"));
        assertTrue(spawns.spawn(ALICE, "mirage").isInstanceRunning());
    }

    @Test
    void stoppingATargetThatIsNotRunningDoesNothing() {
        BoxView view = spawns.stop(ALICE, "sentinel");

        assertFalse(view.isInstanceRunning());
        assertFalse(hypervisor.calls.contains("target-off:sentinel:1"));
    }

    @Test
    void anExpiredTargetIsShownStoppedAndFreesTheSlot() {
        spawns.spawn(ALICE, "sentinel");

        now = NOW.plus(LIFETIME).plusSeconds(1);

        // Lecture : la cible échue n'est plus présentée comme active.
        assertFalse(catalogue.getBox(ALICE, "sentinel").isInstanceRunning());
        // Lancement : la place est libre, et l'ancienne est réellement éteinte.
        assertTrue(spawns.spawn(ALICE, "mirage").isInstanceRunning());
        assertTrue(hypervisor.calls.contains("target-off:sentinel:1"));
        assertFalse(instances.find(1L, 1L).orElseThrow().isRunning());
    }

    @Test
    void anUnknownMachineIsNotFound() {
        assertThrows(NotFoundException.class, () -> spawns.spawn(ALICE, "inexistante"));
        assertThrows(NotFoundException.class, () -> spawns.stop(ALICE, "inexistante"));
    }
}
