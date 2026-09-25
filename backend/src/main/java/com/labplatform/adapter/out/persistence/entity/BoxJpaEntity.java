package com.labplatform.adapter.out.persistence.entity;

import com.labplatform.domain.box.Difficulty;
import com.labplatform.domain.lab.OperatingSystem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Machine du catalogue. Les flags ne sont stockés que sous forme d'empreinte. */
@Entity
@Table(name = "box")
public class BoxJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slug", nullable = false, unique = true, length = 64)
    private String slug;

    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "operating_system", nullable = false, length = 16)
    private OperatingSystem operatingSystem;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 16)
    private Difficulty difficulty;

    @Column(name = "synopsis", nullable = false, length = 512)
    private String synopsis;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "maker", nullable = false, length = 64)
    private String maker;

    @Column(name = "released_at", nullable = false)
    private Instant releasedAt;

    @Column(name = "retired", nullable = false)
    private boolean retired;

    @Column(name = "user_flag_hash", nullable = false, length = 64)
    private String userFlagHash;

    @Column(name = "root_flag_hash", nullable = false, length = 64)
    private String rootFlagHash;

    protected BoxJpaEntity() {
        // requis par JPA
    }

    public BoxJpaEntity(Long id, String slug, String name, OperatingSystem operatingSystem, Difficulty difficulty,
                        String synopsis, String ipAddress, String maker, Instant releasedAt, boolean retired,
                        String userFlagHash, String rootFlagHash) {
        this.id = id;
        this.slug = slug;
        this.name = name;
        this.operatingSystem = operatingSystem;
        this.difficulty = difficulty;
        this.synopsis = synopsis;
        this.ipAddress = ipAddress;
        this.maker = maker;
        this.releasedAt = releasedAt;
        this.retired = retired;
        this.userFlagHash = userFlagHash;
        this.rootFlagHash = rootFlagHash;
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public OperatingSystem getOperatingSystem() {
        return operatingSystem;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getMaker() {
        return maker;
    }

    public Instant getReleasedAt() {
        return releasedAt;
    }

    public boolean isRetired() {
        return retired;
    }

    public String getUserFlagHash() {
        return userFlagHash;
    }

    public String getRootFlagHash() {
        return rootFlagHash;
    }
}
