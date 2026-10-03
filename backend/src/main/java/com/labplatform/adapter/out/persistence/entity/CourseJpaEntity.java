package com.labplatform.adapter.out.persistence.entity;

import com.labplatform.domain.academy.CourseLevel;
import com.labplatform.domain.academy.CourseTopic;
import com.labplatform.domain.academy.Track;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Cours d'une filière. Ses sections, les étapes de son chemin d'attaque et ses
 * concepteurs vivent dans leurs propres tables.
 * <p>
 * La filière est écrite en même temps que le sous-domaine, alors que le domaine
 * la déduit de lui : cette redondance est assumée, c'est par elle qu'on liste
 * une filière sans énumérer ses sous-domaines. Elle vient toujours du même
 * endroit, {@code course.getTrack()}, donc les deux ne peuvent pas diverger.
 */
@Entity
@Table(name = "course")
public class CourseJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slug", nullable = false, unique = true, length = 64)
    private String slug;

    @Column(name = "title", nullable = false, length = 128)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "track", nullable = false, length = 16)
    private Track track;

    @Enumerated(EnumType.STRING)
    @Column(name = "topic", nullable = false, length = 32)
    private CourseTopic topic;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 16)
    private CourseLevel level;

    @Column(name = "summary", nullable = false, length = 512)
    private String summary;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    @Column(name = "attack_summary", length = 1024)
    private String attackSummary;

    @Column(name = "case_sector", length = 128)
    private String caseSector;

    @Column(name = "case_situation", columnDefinition = "text")
    private String caseSituation;

    @Column(name = "case_stake", columnDefinition = "text")
    private String caseStake;

    @Column(name = "case_outcome", columnDefinition = "text")
    private String caseOutcome;

    protected CourseJpaEntity() {
        // requis par JPA
    }

    public CourseJpaEntity(Long id, String slug, String title, Track track, CourseTopic topic, CourseLevel level,
                           String summary, Instant publishedAt, String attackSummary, String caseSector,
                           String caseSituation, String caseStake, String caseOutcome) {
        this.id = id;
        this.slug = slug;
        this.title = title;
        this.track = track;
        this.topic = topic;
        this.level = level;
        this.summary = summary;
        this.publishedAt = publishedAt;
        this.attackSummary = attackSummary;
        this.caseSector = caseSector;
        this.caseSituation = caseSituation;
        this.caseStake = caseStake;
        this.caseOutcome = caseOutcome;
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

    public Track getTrack() {
        return track;
    }

    public CourseLevel getLevel() {
        return level;
    }

    public String getSummary() {
        return summary;
    }

    public CourseTopic getTopic() {
        return topic;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public String getAttackSummary() {
        return attackSummary;
    }

    public String getCaseSector() {
        return caseSector;
    }

    public String getCaseSituation() {
        return caseSituation;
    }

    public String getCaseStake() {
        return caseStake;
    }

    public String getCaseOutcome() {
        return caseOutcome;
    }
}
