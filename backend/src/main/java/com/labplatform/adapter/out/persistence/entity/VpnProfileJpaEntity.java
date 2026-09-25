package com.labplatform.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "vpn_profile")
public class VpnProfileJpaEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "common_name", nullable = false, length = 64, unique = true)
    private String commonName;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    protected VpnProfileJpaEntity() {
        // requis par JPA
    }

    public VpnProfileJpaEntity(Long userId, String commonName, Instant issuedAt) {
        this.userId = userId;
        this.commonName = commonName;
        this.issuedAt = issuedAt;
    }

    public Long getUserId() {
        return userId;
    }

    public String getCommonName() {
        return commonName;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }
}
