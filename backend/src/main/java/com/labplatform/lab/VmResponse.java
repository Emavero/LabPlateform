package com.labplatform.lab;

/**
 * DTO exposé à l'API. Les informations de connexion (ip/port/identifiants)
 * ne sont renseignées que lorsque la machine est démarrée.
 */
public record VmResponse(
        Long id,
        VmType type,
        VmStatus status,
        String ipAddress,
        Integer port,
        String username,
        String accessPassword
) {
    public static VmResponse from(VirtualMachine vm) {
        boolean running = vm.getStatus() == VmStatus.RUNNING;
        return new VmResponse(
                vm.getId(),
                vm.getType(),
                vm.getStatus(),
                running ? vm.getIpAddress() : null,
                running ? vm.getPort() : null,
                running ? vm.getUsername() : null,
                running ? vm.getAccessPassword() : null
        );
    }
}
