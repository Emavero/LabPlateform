package com.labplatform.application.port.in.admin;

import com.labplatform.domain.academy.CourseLevel;
import com.labplatform.domain.academy.SectionKind;
import com.labplatform.domain.academy.Track;

import java.util.List;

/**
 * Cours tel que l'administrateur le saisit. Ni identifiant de cours, ni
 * position de section : le premier vient de la base, la seconde de l'ordre
 * des sections dans la liste.
 */
public record CourseDraft(String title, Track track, CourseLevel level, String summary,
                          List<SectionDraft> sections) {

    /**
     * Section saisie. L'identifiant, renvoyé par l'éditeur pour une section
     * existante, est ce qui permet de renommer une section sans effacer
     * l'avancement de ceux qui l'avaient terminée.
     */
    public record SectionDraft(Long id, String title, SectionKind kind, int minutes, String content,
                               String videoUrl, List<QuestionDraft> questions) {

        /** Sans quiz. */
        public SectionDraft(Long id, String title, SectionKind kind, int minutes, String content, String videoUrl) {
            this(id, title, kind, minutes, content, videoUrl, List.of());
        }

        public List<QuestionDraft> questions() {
            return questions == null ? List.of() : questions;
        }
    }

    /** Question saisie : un énoncé et ses propositions, dont au moins une correcte. */
    public record QuestionDraft(String statement, List<ChoiceDraft> choices) {
    }

    public record ChoiceDraft(String label, boolean correct) {
    }
}
