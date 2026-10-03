package com.labplatform.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Étape du chemin d'attaque d'un cours. */
@Entity
@Table(name = "course_attack_stage")
public class CourseAttackStageJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "technique", length = 128)
    private String technique;

    protected CourseAttackStageJpaEntity() {
        // requis par JPA
    }

    public CourseAttackStageJpaEntity(Long id, Long courseId, int position, String name, String description,
                                      String technique) {
        this.id = id;
        this.courseId = courseId;
        this.position = position;
        this.name = name;
        this.description = description;
        this.technique = technique;
    }

    public Long getId() {
        return id;
    }

    public Long getCourseId() {
        return courseId;
    }

    public int getPosition() {
        return position;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getTechnique() {
        return technique;
    }
}
