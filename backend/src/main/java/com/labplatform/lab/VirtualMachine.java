package com.labplatform.lab;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Représente une machine virtuelle attribuée à un utilisateur.
 * Dans une future itération, ce module pourra déléguer le provisionnement
 * réel à un service d'orchestration (ex: appel à un hyperviseur / API cloud)
 * sans changer ce modèle ni le contrat exposé par LabController.
 */
@Entity
@Table(name = "virtual_machine")
@Getter
@Setter
@NoArgsConstructor
public class VirtualMachine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VmType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VmStatus status = VmStatus.STOPPED;

    private String ipAddress;
    private Integer port;
    private String username;
    private String accessPassword;

    public VirtualMachine(Long userId, VmType type) {
        this.userId = userId;
        this.type = type;
        this.status = VmStatus.STOPPED;
    }
}
