package com.labplatform.adapter.out.persistence.entity;

import com.labplatform.domain.box.Difficulty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Vote de difficulté d'un joueur sur une machine. Un seul par couple. */
@Entity
@Table(name = "box_rating")
public class BoxRatingJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "box_id", nullable = false)
    private Long boxId;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false, length = 16)
    private Difficulty difficulty;

    @Column(name = "rated_at", nullable = false)
    private Instant ratedAt;

    protected BoxRatingJpaEntity() {
        // requis par JPA
    }

    public BoxRatingJpaEntity(Long id, Long userId, Long boxId, Difficulty difficulty, Instant ratedAt) {
        this.id = id;
        this.userId = userId;
        this.boxId = boxId;
        this.difficulty = difficulty;
        this.ratedAt = ratedAt;
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

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public Instant getRatedAt() {
        return ratedAt;
    }
}
