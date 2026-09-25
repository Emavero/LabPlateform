package com.labplatform.adapter.out.persistence.entity;

import com.labplatform.domain.lab.AccessProtocol;
import com.labplatform.domain.lab.OperatingSystem;
import com.labplatform.domain.lab.VmStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "virtual_machine")
public class VirtualMachineJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operating_system", nullable = false, length = 16)
    private OperatingSystem operatingSystem;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private VmStatus status;

    @Column(name = "host", length = 255)
    private String host;

    @Column(name = "port")
    private Integer port;

    @Enumerated(EnumType.STRING)
    @Column(name = "protocol", length = 8)
    private AccessProtocol protocol;

    @Column(name = "access_username", length = 64)
    private String accessUsername;

    @Column(name = "access_password", length = 128)
    private String accessPassword;

    @Column(name = "started_at")
    private Instant startedAt;

    protected VirtualMachineJpaEntity() {
        // requis par JPA
    }

    public VirtualMachineJpaEntity(Long id, Long ownerId, OperatingSystem operatingSystem, VmStatus status,
                                   String host, Integer port, AccessProtocol protocol, String accessUsername,
                                   String accessPassword, Instant startedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.operatingSystem = operatingSystem;
        this.status = status;
        this.host = host;
        this.port = port;
        this.protocol = protocol;
        this.accessUsername = accessUsername;
        this.accessPassword = accessPassword;
        this.startedAt = startedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public OperatingSystem getOperatingSystem() {
        return operatingSystem;
    }

    public VmStatus getStatus() {
        return status;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public AccessProtocol getProtocol() {
        return protocol;
    }

    public String getAccessUsername() {
        return accessUsername;
    }

    public String getAccessPassword() {
        return accessPassword;
    }

    public Instant getStartedAt() {
        return startedAt;
    }
}
