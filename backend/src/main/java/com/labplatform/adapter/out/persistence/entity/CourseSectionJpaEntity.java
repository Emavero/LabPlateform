package com.labplatform.adapter.out.persistence.entity;

import com.labplatform.domain.academy.SectionKind;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Section d'un cours. Le contenu est du texte long, rendu tel quel côté client. */
@Entity
@Table(name = "course_section")
public class CourseSectionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "slug", nullable = false, length = 64)
    private String slug;

    @Column(name = "title", nullable = false, length = 128)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 8)
    private SectionKind kind;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "minutes", nullable = false)
    private int minutes;

    /*
     * Surtout pas @Lob : sur PostgreSQL, Hibernate écrirait alors un « large
     * object » et ne stockerait ici que son identifiant, illisible hors
     * transaction (« Large Objects may not be used in auto-commit mode »).
     * La colonne est du TEXT ordinaire, des deux côtés.
     */
    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "video_url", length = 512)
    private String videoUrl;

    protected CourseSectionJpaEntity() {
        // requis par JPA
    }

    public CourseSectionJpaEntity(Long id, Long courseId, String slug, String title, SectionKind kind, int position,
                                  int minutes, String content, String videoUrl) {
        this.id = id;
        this.courseId = courseId;
        this.slug = slug;
        this.title = title;
        this.kind = kind;
        this.position = position;
        this.minutes = minutes;
        this.content = content;
        this.videoUrl = videoUrl;
    }

    public Long getId() {
        return id;
    }

    public Long getCourseId() {
        return courseId;
    }

    public String getSlug() {
        return slug;
    }

    public String getTitle() {
        return title;
    }

    public SectionKind getKind() {
        return kind;
    }

    public int getPosition() {
        return position;
    }

    public int getMinutes() {
        return minutes;
    }

    public String getContent() {
        return content;
    }

    public String getVideoUrl() {
        return videoUrl;
    }
}
