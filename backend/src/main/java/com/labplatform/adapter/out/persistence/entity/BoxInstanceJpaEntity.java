package com.labplatform.adapter.out.persistence.entity;

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

/** Cible lancée à la demande. Une ligne par couple (joueur, machine). */
@Entity
@Table(name = "box_instance")
public class BoxInstanceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "box_id", nullable = false)
    private Long boxId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private VmStatus status;

    @Column(name = "address", length = 45)
    private String address;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    protected BoxInstanceJpaEntity() {
        // requis par JPA
    }

    public BoxInstanceJpaEntity(Long id, Long userId, Long boxId, VmStatus status, String address,
                                Instant startedAt, Instant expiresAt) {
        this.id = id;
        this.userId = userId;
        this.boxId = boxId;
        this.status = status;
        this.address = address;
        this.startedAt = startedAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getBoxId() {
        return boxId;
    }

    public VmStatus getStatus() {
        return status;
    }

    public String getAddress() {
        return address;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
