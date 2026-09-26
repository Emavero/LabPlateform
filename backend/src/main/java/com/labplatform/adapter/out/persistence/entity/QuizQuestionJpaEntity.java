package com.labplatform.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Question d'un quiz, rattachée à une section de cours. */
@Entity
@Table(name = "quiz_question")
public class QuizQuestionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "section_id", nullable = false)
    private Long sectionId;

    @Column(name = "statement", nullable = false, length = 512)
    private String statement;

    @Column(name = "position", nullable = false)
    private int position;

    protected QuizQuestionJpaEntity() {
        // requis par JPA
    }

    public QuizQuestionJpaEntity(Long id, Long sectionId, String statement, int position) {
        this.id = id;
        this.sectionId = sectionId;
        this.statement = statement;
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public Long getSectionId() {
        return sectionId;
    }

    public String getStatement() {
        return statement;
    }

    public int getPosition() {
        return position;
    }
}
