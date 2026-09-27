package com.labplatform.adapter.out.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Scénario d'exercice et ses étapes.
 * <p>
 * Les étapes sont en cascade et sans existence propre : réécrire un scénario les
 * remplace en bloc, ce qui est exactement ce que fait l'éditeur — une étape n'a
 * pas d'identité que le lecteur suivrait d'une version à l'autre.
 */
@Entity
@Table(name = "scenario")
public class ScenarioJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slug", nullable = false, unique = true, length = 64)
    private String slug;

    @Column(name = "title", nullable = false, length = 140)
    private String title;

    /* Pas de @Lob : sur PostgreSQL il écrirait un « large object » (voir docs/ARCHITECTURE.md). */
    @Column(name = "brief", nullable = false, columnDefinition = "text")
    private String brief;

    @Column(name = "published", nullable = false)
    private boolean published;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "scenario_id", nullable = false)
    @OrderBy("position ASC")
    private List<ScenarioStepJpaEntity> steps = new ArrayList<>();

    protected ScenarioJpaEntity() {
        // requis par JPA
    }

    public ScenarioJpaEntity(Long id, String slug, String title, String brief, boolean published, Instant createdAt,
                             Instant updatedAt) {
        this.id = id;
        this.slug = slug;
        this.title = title;
        this.brief = brief;
        this.published = published;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void update(String title, String brief, boolean published, Instant updatedAt) {
        this.title = title;
        this.brief = brief;
        this.published = published;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public String getBrief() {
        return brief;
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

    public List<ScenarioStepJpaEntity> getSteps() {
        return steps;
    }
}
