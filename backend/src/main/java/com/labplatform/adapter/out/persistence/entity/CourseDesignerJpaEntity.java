package com.labplatform.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Concepteur du scénario d'un cours. */
@Entity
@Table(name = "course_designer")
public class CourseDesignerJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "role", nullable = false, length = 128)
    private String role;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    protected CourseDesignerJpaEntity() {
        // requis par JPA
    }

    public CourseDesignerJpaEntity(Long id, Long courseId, int position, String name, String role, String avatarUrl) {
        this.id = id;
        this.courseId = courseId;
        this.position = position;
        this.name = name;
        this.role = role;
        this.avatarUrl = avatarUrl;
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

    public String getRole() {
        return role;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }
}
