package com.labplatform.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Compte rendu d'un joueur sur une machine. Un par couple (auteur, machine). */
@Entity
@Table(name = "writeup")
public class WriteupJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "box_id", nullable = false)
    private Long boxId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "title", nullable = false, length = 128)
    private String title;

    /* Pas de @Lob : sur PostgreSQL il écrirait un « large object » (voir docs/ARCHITECTURE.md). */
    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "published", nullable = false)
    private boolean published;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected WriteupJpaEntity() {
        // requis par JPA
    }

    public WriteupJpaEntity(Long id, Long boxId, Long authorId, String title, String content, boolean published,
                            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.boxId = boxId;
        this.authorId = authorId;
        this.title = title;
        this.content = content;
        this.published = published;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getBoxId() {
        return boxId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public boolean isPublished() {
        return published;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
