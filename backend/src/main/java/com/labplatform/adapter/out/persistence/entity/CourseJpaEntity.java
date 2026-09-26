package com.labplatform.adapter.out.persistence.entity;

import com.labplatform.domain.academy.CourseLevel;
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

/** Cours d'une filière. Ses sections vivent dans course_section. */
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
    @Column(name = "level", nullable = false, length = 16)
    private CourseLevel level;

    @Column(name = "summary", nullable = false, length = 512)
    private String summary;

    @Column(name = "published_at", nullable = false)
    private Instant publishedAt;

    protected CourseJpaEntity() {
        // requis par JPA
    }

    public CourseJpaEntity(Long id, String slug, String title, Track track, CourseLevel level, String summary,
                           Instant publishedAt) {
        this.id = id;
        this.slug = slug;
        this.title = title;
        this.track = track;
        this.level = level;
        this.summary = summary;
        this.publishedAt = publishedAt;
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

    public Instant getPublishedAt() {
        return publishedAt;
    }
}
