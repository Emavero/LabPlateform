package com.labplatform.domain.lab;

import com.labplatform.domain.shared.ConflictException;
import com.labplatform.domain.shared.NotFoundException;
import com.labplatform.domain.user.Actor;
import com.labplatform.domain.user.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualMachineTest {

    private static final Instant NOW = Instant.parse("2026-09-24T10:00:00Z");
    private static final ConnectionInfo SSH = new ConnectionInfo("10.42.1.2", 22, AccessProtocol.SSH, "labuser", "secret");

    @Test
    void newMachineIsStoppedWithoutConnectionInfo() {
        VirtualMachine vm = VirtualMachine.provision(1L, OperatingSystem.LINUX);

        assertEquals(VmStatus.STOPPED, vm.getStatus());
        assertTrue(vm.getConnection().isEmpty());
        assertTrue(vm.getStartedAt().isEmpty());
    }

    @Test
    void startingExposesConnectionInfo() {
        VirtualMachine vm = VirtualMachine.provision(1L, OperatingSystem.LINUX);

        vm.markStarted(SSH, NOW);

        assertTrue(vm.isRunning());
        assertEquals(SSH, vm.getConnection().orElseThrow());
        assertEquals(NOW, vm.getStartedAt().orElseThrow());
    }

    @Test
    void stoppingClearsConnectionInfo() {
        VirtualMachine vm = VirtualMachine.provision(1L, OperatingSystem.LINUX);
        vm.markStarted(SSH, NOW);

        vm.markStopped();

        assertEquals(VmStatus.STOPPED, vm.getStatus());
        assertTrue(vm.getConnection().isEmpty());
        assertTrue(vm.getStartedAt().isEmpty());
    }

    @Test
    void cannotStartTwice() {
        VirtualMachine vm = VirtualMachine.provision(1L, OperatingSystem.LINUX);
        vm.markStarted(SSH, NOW);

        assertThrows(ConflictException.class, () -> vm.markStarted(SSH, NOW));
    }

    @Test
    void cannotStopAStoppedMachine() {
        VirtualMachine vm = VirtualMachine.provision(1L, OperatingSystem.WINDOWS);

        assertThrows(ConflictException.class, vm::markStopped);
    }

    @Test
    void rejectsConnectionInfoForTheWrongProtocol() {
        VirtualMachine windows = VirtualMachine.provision(1L, OperatingSystem.WINDOWS);

        assertThrows(IllegalArgumentException.class, () -> windows.markStarted(SSH, NOW));
        assertFalse(windows.isRunning());
    }

    @Test
    void restoringAnInconsistentStateIsRejected() {
        assertThrows(IllegalStateException.class,
                () -> VirtualMachine.restore(5L, 1L, OperatingSystem.LINUX, VmStatus.RUNNING, null, NOW));
    }

    @Test
    void onlyOwnerOrAdminCanOperate() {
        VirtualMachine vm = VirtualMachine.provision(1L, OperatingSystem.LINUX);

        assertSame(vm, VmAccessPolicy.requireAccess(new Actor(1L, Role.USER), vm));
        assertSame(vm, VmAccessPolicy.requireAccess(new Actor(99L, Role.ADMIN), vm));
        assertThrows(NotFoundException.class, () -> VmAccessPolicy.requireAccess(new Actor(2L, Role.USER), vm));
    }

    @Test
    void connectionInfoNeverPrintsThePassword() {
        assertFalse(SSH.toString().contains("secret"));
    }
}
