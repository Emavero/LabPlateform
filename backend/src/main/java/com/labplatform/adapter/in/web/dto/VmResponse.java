package com.labplatform.adapter.in.web.dto;

import com.labplatform.domain.lab.ConnectionInfo;
import com.labplatform.domain.lab.VirtualMachine;

import java.time.Instant;

/**
 * Représentation HTTP d'une machine. "connection" vaut null tant que la
 * machine est arrêtée : c'est le domaine qui garantit cet invariant.
 */
public record VmResponse(
        Long id,
        String os,
        String osName,
        String status,
        Instant startedAt,
        Connection connection) {

    public record Connection(String host, int port, String protocol, String username, String password) {

        static Connection from(ConnectionInfo info) {
            return new Connection(info.host(), info.port(), info.protocol().name(), info.username(), info.password());
        }
    }

    public static VmResponse from(VirtualMachine vm) {
        return new VmResponse(
                vm.getId(),
                vm.getOperatingSystem().name(),
                vm.getOperatingSystem().displayName(),
                vm.getStatus().name(),
                vm.getStartedAt().orElse(null),
                vm.getConnection().map(Connection::from).orElse(null));
    }
}
