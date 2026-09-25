package com.labplatform.application.fakes;

import com.labplatform.application.port.in.auth.UserSummary;
import com.labplatform.application.port.out.AccessTokenIssuerPort;
import com.labplatform.application.port.out.HypervisorPort;
import com.labplatform.application.port.out.PasswordHasherPort;
import com.labplatform.application.port.out.TransactionPort;
import com.labplatform.domain.lab.ConnectionInfo;
import com.labplatform.domain.lab.VirtualMachine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Doublures minimalistes des ports sortants, pour tester les cas d'usage sans Spring ni base. */
public final class Fakes {

    private Fakes() {
    }

    public static final TransactionPort NO_TRANSACTION = new TransactionPort() {
        @Override
        public <T> T inTransaction(Supplier<T> work) {
            return work.get();
        }
    };

    public static final PasswordHasherPort REVERSING_HASHER = new PasswordHasherPort() {
        @Override
        public String hash(String raw) {
            return "hashed:" + new StringBuilder(raw).reverse();
        }

        @Override
        public boolean matches(String raw, String hash) {
            return hash(raw).equals(hash);
        }
    };

    public static final AccessTokenIssuerPort TOKEN_ISSUER = (UserSummary user) -> "token-for-" + user.id();

    public static class RecordingHypervisor implements HypervisorPort {
        public final List<String> calls = new ArrayList<>();

        @Override
        public ConnectionInfo powerOn(VirtualMachine vm) {
            calls.add("on:" + vm.getId());
            var protocol = vm.getOperatingSystem().protocol();
            return new ConnectionInfo("10.0.0." + vm.getId(), protocol.defaultPort(), protocol, "labuser", "pw");
        }

        @Override
        public void powerOff(VirtualMachine vm) {
            calls.add("off:" + vm.getId());
        }

        @Override
        public List<String> consoleLog(VirtualMachine vm) {
            return List.of("log:" + vm.getId());
        }
    }
}
