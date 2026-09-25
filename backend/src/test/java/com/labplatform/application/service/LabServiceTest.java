package com.labplatform.application.service;

import com.labplatform.application.fakes.Fakes;
import com.labplatform.application.fakes.InMemoryMachines;
import com.labplatform.domain.lab.AccessProtocol;
import com.labplatform.domain.lab.ConnectionInfo;
import com.labplatform.domain.lab.OperatingSystem;
import com.labplatform.domain.lab.VirtualMachine;
import com.labplatform.domain.shared.ConflictException;
import com.labplatform.domain.shared.NotFoundException;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LabServiceTest {

    private static final Actor ALICE = new Actor(1L, Role.USER);
    private static final Actor BOB = new Actor(2L, Role.USER);

    private InMemoryMachines machines;
    private Fakes.RecordingHypervisor hypervisor;
    private LabService lab;

    @BeforeEach
    void setUp() {
        machines = new InMemoryMachines();
        hypervisor = new Fakes.RecordingHypervisor();
        Clock clock = Clock.fixed(Instant.parse("2026-09-24T10:00:00Z"), ZoneOffset.UTC);
        lab = new LabService(machines, hypervisor, Fakes.NO_TRANSACTION, clock);
    }

    @Test
    void listingProvisionsOneWindowsAndOneLinuxMachineOnce() {
        List<VirtualMachine> first = lab.listMachines(ALICE);
        List<VirtualMachine> second = lab.listMachines(ALICE);

        assertEquals(List.of(OperatingSystem.WINDOWS, OperatingSystem.LINUX),
                first.stream().map(VirtualMachine::getOperatingSystem).toList());
        assertEquals(2, second.size());
        assertEquals(2, machines.count());
    }

    @Test
    void startingAMachineReturnsItsAccessDetails() {
        VirtualMachine windows = lab.listMachines(ALICE).get(0);

        VirtualMachine started = lab.start(ALICE, windows.getId());

        ConnectionInfo access = started.getConnection().orElseThrow();
        assertTrue(started.isRunning());
        assertEquals(AccessProtocol.RDP, access.protocol());
        assertEquals(3389, access.port());
        assertEquals(List.of("on:" + windows.getId()), hypervisor.calls);
    }

    @Test
    void stoppingAMachineClearsItsAccessDetails() {
        VirtualMachine linux = lab.listMachines(ALICE).get(1);
        lab.start(ALICE, linux.getId());

        VirtualMachine stopped = lab.stop(ALICE, linux.getId());

        assertTrue(stopped.getConnection().isEmpty());
        assertEquals(List.of("on:" + linux.getId(), "off:" + linux.getId()), hypervisor.calls);
    }

    @Test
    void theHypervisorIsNotCalledWhenTheTransitionIsInvalid() {
        VirtualMachine linux = lab.listMachines(ALICE).get(1);

        assertThrows(ConflictException.class, () -> lab.stop(ALICE, linux.getId()));
        assertTrue(hypervisor.calls.isEmpty());
    }

    @Test
    void usersCannotSeeOrOperateSomeoneElsesMachines() {
        VirtualMachine alicesVm = lab.listMachines(ALICE).get(0);

        assertThrows(NotFoundException.class, () -> lab.getMachine(BOB, alicesVm.getId()));
        assertThrows(NotFoundException.class, () -> lab.start(BOB, alicesVm.getId()));
        assertTrue(hypervisor.calls.isEmpty());
    }
}
