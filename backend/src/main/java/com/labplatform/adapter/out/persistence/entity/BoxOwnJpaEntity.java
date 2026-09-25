package com.labplatform.adapter.out.persistence.entity;

import com.labplatform.domain.box.FlagKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Validation d'un flag par un joueur. L'unicité (joueur, machine, flag) est tenue en base. */
@Entity
@Table(name = "box_own")
public class BoxOwnJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "box_id", nullable = false)
    private Long boxId;

    @Enumerated(EnumType.STRING)
    @Column(name = "flag_kind", nullable = false, length = 8)
    private FlagKind kind;

    @Column(name = "points", nullable = false)
    private int points;

    @Column(name = "first_blood", nullable = false)
    private boolean firstBlood;

    @Column(name = "owned_at", nullable = false)
    private Instant ownedAt;

    protected BoxOwnJpaEntity() {
        // requis par JPA
    }

    public BoxOwnJpaEntity(Long id, Long userId, Long boxId, FlagKind kind, int points, boolean firstBlood,
                           Instant ownedAt) {
        this.id = id;
        this.userId = userId;
        this.boxId = boxId;
        this.kind = kind;
        this.points = points;
        this.firstBlood = firstBlood;
        this.ownedAt = ownedAt;
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

    public FlagKind getKind() {
        return kind;
    }

    public int getPoints() {
        return points;
    }

    public boolean isFirstBlood() {
        return firstBlood;
    }

    public Instant getOwnedAt() {
        return ownedAt;
    }
}
