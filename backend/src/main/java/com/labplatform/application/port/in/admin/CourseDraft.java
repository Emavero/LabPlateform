package com.labplatform.application.port.in.admin;

import com.labplatform.domain.academy.CourseLevel;
import com.labplatform.domain.academy.CourseTopic;
import com.labplatform.domain.academy.SectionKind;

import java.util.List;

/**
 * Cours tel que l'administrateur le saisit. Ni identifiant de cours, ni
 * position de section : le premier vient de la base, la seconde de l'ordre
 * des sections dans la liste.
 * <p>
 * La filière n'est pas saisie : elle découle du sous-domaine choisi. L'éditeur
 * fait choisir la filière avant le sous-domaine, mais ce n'est là qu'une façon
 * de raccourcir la liste — seul le second est envoyé.
 */
public record CourseDraft(String title, CourseTopic topic, CourseLevel level, String summary,
                          List<SectionDraft> sections, BriefingDraft briefing) {

    /** Sans chemin d'attaque, cas d'usage ni concepteurs. */
    public CourseDraft(String title, CourseTopic topic, CourseLevel level, String summary,
                       List<SectionDraft> sections) {
        this(title, topic, level, summary, sections, BriefingDraft.empty());
    }

    public BriefingDraft briefing() {
        return briefing == null ? BriefingDraft.empty() : briefing;
    }

    /**
     * Ce qui entoure les sections : la chaîne d'attaque étudiée, la situation
     * réelle où elle se rencontre, et ceux qui l'ont écrite. Facultatif d'un
     * bout à l'autre — un cours reste publiable sans.
     */
    public record BriefingDraft(String attackSummary, List<StageDraft> stages, CaseDraft realCase,
                                List<DesignerDraft> designers) {

        private static final BriefingDraft EMPTY = new BriefingDraft(null, List.of(), null, List.of());

        public static BriefingDraft empty() {
            return EMPTY;
        }

        public List<StageDraft> stages() {
            return stages == null ? List.of() : stages;
        }

        public List<DesignerDraft> designers() {
            return designers == null ? List.of() : designers;
        }
    }

    /** Étape de la chaîne d'attaque. Sa position vient de l'ordre de la liste. */
    public record StageDraft(String name, String description, String technique) {
    }

    /** Mise en situation : chez qui, quoi, quel enjeu, quelle issue. */
    public record CaseDraft(String sector, String situation, String stake, String outcome) {
    }

    /** Concepteur du scénario : son nom, son rôle, son avatar. */
    public record DesignerDraft(String name, String role, String avatarUrl) {
    }

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
