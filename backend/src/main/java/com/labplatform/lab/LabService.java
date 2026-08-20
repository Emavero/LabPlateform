package com.labplatform.lab;

import com.labplatform.auth.UserRegisteredEvent;
import com.labplatform.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class LabService {

    private final VmRepository vmRepository;

    /**
     * Provisionne automatiquement une VM Windows et une VM Linux pour chaque
     * nouvel utilisateur. Écoute l'événement publié par le module auth :
     * aucun couplage direct entre les deux modules.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        provisionDefaultVms(event.userId());
    }

    @Transactional
    public void provisionDefaultVms(Long userId) {
        vmRepository.save(new VirtualMachine(userId, VmType.WINDOWS));
        vmRepository.save(new VirtualMachine(userId, VmType.LINUX));
    }

    @Transactional(readOnly = true)
    public List<VmResponse> listForUser(Long userId) {
        return vmRepository.findByUserId(userId).stream()
                .map(VmResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public VmResponse getOne(Long userId, Long vmId) {
        return VmResponse.from(requireOwnedVm(userId, vmId));
    }

    /**
     * Journal de console simulé. À remplacer par un flux réel (WebSocket vers
     * l'hyperviseur / API cloud) le jour où le provisionnement est branché.
     */
    @Transactional(readOnly = true)
    public List<String> getLogs(Long userId, Long vmId) {
        VirtualMachine vm = requireOwnedVm(userId, vmId);

        if (vm.getStatus() == VmStatus.STOPPED) {
            return List.of("[info] Machine arrêtée — aucun journal disponible.");
        }

        String osLabel = vm.getType() == VmType.WINDOWS ? "Windows Server" : "Ubuntu Linux";
        return List.of(
                "[boot] Démarrage de la machine (" + osLabel + ")…",
                "[net]  Attribution de l'adresse IP " + vm.getIpAddress(),
                "[net]  Service d'accès distant en écoute sur le port " + vm.getPort(),
                "[auth] Compte '" + vm.getUsername() + "' prêt",
                "[ok]   Machine disponible et prête à l'emploi"
        );
    }

    @Transactional
    public VmResponse start(Long userId, Long vmId) {
        VirtualMachine vm = requireOwnedVm(userId, vmId);

        if (vm.getStatus() == VmStatus.RUNNING) {
            throw new BusinessException("La machine est déjà en cours d'exécution", HttpStatus.CONFLICT);
        }

        // Simulation du provisionnement : dans une version réelle, un appel
        // serait fait ici vers l'infrastructure distante (hyperviseur / API cloud).
        vm.setStatus(VmStatus.RUNNING);
        vm.setIpAddress(randomLabIp());
        vm.setPort(vm.getType() == VmType.WINDOWS ? 3389 : 22);
        vm.setUsername(vm.getType() == VmType.WINDOWS ? "labuser" : "labuser");
        vm.setAccessPassword(randomPassword());

        return VmResponse.from(vmRepository.save(vm));
    }

    @Transactional
    public VmResponse stop(Long userId, Long vmId) {
        VirtualMachine vm = requireOwnedVm(userId, vmId);

        if (vm.getStatus() == VmStatus.STOPPED) {
            throw new BusinessException("La machine est déjà arrêtée", HttpStatus.CONFLICT);
        }

        vm.setStatus(VmStatus.STOPPED);
        vm.setIpAddress(null);
        vm.setPort(null);
        vm.setUsername(null);
        vm.setAccessPassword(null);

        return VmResponse.from(vmRepository.save(vm));
    }

    private VirtualMachine requireOwnedVm(Long userId, Long vmId) {
        return vmRepository.findByIdAndUserId(vmId, userId)
                .orElseThrow(() -> new BusinessException("Machine introuvable ou accès non autorisé", HttpStatus.NOT_FOUND));
    }

    private String randomLabIp() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        return "10.42.%d.%d".formatted(r.nextInt(1, 255), r.nextInt(2, 254));
    }

    private String randomPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        ThreadLocalRandom r = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
