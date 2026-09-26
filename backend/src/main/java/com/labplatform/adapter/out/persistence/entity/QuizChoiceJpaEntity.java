package com.labplatform.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Proposition d'une question. La colonne « correct » ne sort jamais telle quelle. */
@Entity
@Table(name = "quiz_choice")
public class QuizChoiceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "question_id", nullable = false)
    private Long questionId;

    @Column(name = "label", nullable = false, length = 256)
    private String label;

    @Column(name = "correct", nullable = false)
    private boolean correct;

    @Column(name = "position", nullable = false)
    private int position;

    protected QuizChoiceJpaEntity() {
        // requis par JPA
    }

    public QuizChoiceJpaEntity(Long id, Long questionId, String label, boolean correct, int position) {
        this.id = id;
        this.questionId = questionId;
        this.label = label;
        this.correct = correct;
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public String getLabel() {
        return label;
    }

    public boolean isCorrect() {
        return correct;
    }

    public int getPosition() {
        return position;
    }
}
