package com.labplatform.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Section cochée par un apprenant. L'unicité (apprenant, section) est tenue en base. */
@Entity
@Table(name = "course_section_completion")
public class SectionCompletionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    protected SectionCompletionJpaEntity() {
        // requis par JPA
    }

    public SectionCompletionJpaEntity(Long id, Long userId, Long courseId, Long sectionId, Instant completedAt) {
        this.id = id;
        this.userId = userId;
        this.courseId = courseId;
        this.sectionId = sectionId;
        this.completedAt = completedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getCourseId() {
        return courseId;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
