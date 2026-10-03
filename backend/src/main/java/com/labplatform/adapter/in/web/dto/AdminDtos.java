package com.labplatform.adapter.in.web.dto;

import com.labplatform.adapter.in.web.Texts;
import com.labplatform.application.port.in.admin.AdminOverview;
import com.labplatform.application.port.in.admin.BoxDraft;
import com.labplatform.application.port.in.admin.CourseDraft;
import com.labplatform.application.port.in.admin.PublishedBox;
import com.labplatform.domain.academy.CourseLevel;
import com.labplatform.domain.academy.CourseTopic;
import com.labplatform.domain.academy.SectionKind;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.Difficulty;
import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.lab.OperatingSystem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

/** Représentations HTTP du tableau de bord d'administration. */
public final class AdminDtos {

    private AdminDtos() {
    }

    /**
     * Cours saisi dans l'éditeur. La filière n'est pas transmise : le
     * sous-domaine la porte.
     */
    public record CourseDraftRequest(
            @NotBlank(message = "Le titre est obligatoire")
            @Size(max = 128, message = "Le titre est limité à 128 caractères") String title,
            @NotNull(message = "Le sous-domaine est obligatoire") CourseTopic topic,
            @NotNull(message = "Le niveau est obligatoire") CourseLevel level,
            @Size(max = 512, message = "Le résumé est limité à 512 caractères") String summary,
            @NotEmpty(message = "Un cours comporte au moins une section")
            @Valid List<SectionDraftRequest> sections,
            @Valid BriefingRequest briefing) {

        public CourseDraft toDraft() {
            return new CourseDraft(title, topic, level, summary,
                    sections.stream().map(SectionDraftRequest::toDraft).toList(),
                    briefing == null ? CourseDraft.BriefingDraft.empty() : briefing.toDraft());
        }
    }

    /** Chemin d'attaque, cas d'usage réel et concepteurs : tout est facultatif. */
    public record BriefingRequest(
            @Size(max = 1024, message = "Le résumé du chemin d'attaque est limité à 1024 caractères")
            String attackSummary,
            @Valid List<StageRequest> stages,
            @Valid CaseRequest realCase,
            @Valid List<DesignerRequest> designers) {

        CourseDraft.BriefingDraft toDraft() {
            return new CourseDraft.BriefingDraft(attackSummary,
                    stages == null ? List.of() : stages.stream().map(StageRequest::toDraft).toList(),
                    realCase == null ? null : realCase.toDraft(),
                    designers == null ? List.of() : designers.stream().map(DesignerRequest::toDraft).toList());
        }
    }

    /**
     * Étape de la chaîne d'attaque. Rien n'est obligatoire ici : une ligne
     * laissée vide est écartée, seule une ligne à moitié remplie est refusée —
     * et c'est le domaine qui le dit.
     */
    public record StageRequest(
            @Size(max = 128, message = "Le nom de l'étape est limité à 128 caractères") String name,
            @Size(max = 1024, message = "La description est limitée à 1024 caractères") String description,
            @Size(max = 128, message = "La technique est limitée à 128 caractères") String technique) {

        CourseDraft.StageDraft toDraft() {
            return new CourseDraft.StageDraft(name, description, technique);
        }
    }

    public record CaseRequest(
            @Size(max = 128, message = "Le secteur est limité à 128 caractères") String sector,
            @Size(max = 2048, message = "La situation est limitée à 2048 caractères") String situation,
            @Size(max = 1024, message = "L'enjeu est limité à 1024 caractères") String stake,
            @Size(max = 1024, message = "L'issue est limitée à 1024 caractères") String outcome) {

        CourseDraft.CaseDraft toDraft() {
            return new CourseDraft.CaseDraft(sector, situation, stake, outcome);
        }
    }

    public record DesignerRequest(
            @Size(max = 128, message = "Le nom est limité à 128 caractères") String name,
            @Size(max = 128, message = "Le rôle est limité à 128 caractères") String role,
            @Size(max = 512, message = "L'adresse de l'avatar est trop longue") String avatarUrl) {

        CourseDraft.DesignerDraft toDraft() {
            return new CourseDraft.DesignerDraft(name, role, avatarUrl);
        }
    }

    /**
     * Section saisie. L'identifiant est celui que la lecture du cours a
     * renvoyé : le laisser vide crée une nouvelle section.
     */
    public record SectionDraftRequest(
            Long id,
            @NotBlank(message = "Le titre de la section est obligatoire")
            @Size(max = 128, message = "Le titre est limité à 128 caractères") String title,
            @NotNull(message = "Le type de section est obligatoire") SectionKind kind,
            @Min(value = 0, message = "Une durée est positive")
            @Max(value = 600, message = "Une section dépasse rarement dix heures") int minutes,
            String content,
            @Size(max = 512, message = "L'adresse de la vidéo est trop longue") String videoUrl,
            @Valid List<QuestionDraftRequest> questions) {

        CourseDraft.SectionDraft toDraft() {
            List<CourseDraft.QuestionDraft> quiz = questions == null
                    ? List.of()
                    : questions.stream().map(QuestionDraftRequest::toDraft).toList();
            return new CourseDraft.SectionDraft(id, title, kind, minutes, content, videoUrl, quiz);
        }
    }

    /** Question de quiz saisie : un énoncé et ses propositions. */
    public record QuestionDraftRequest(
            @NotBlank(message = "L'énoncé de la question est obligatoire")
            @Size(max = 512, message = "L'énoncé est limité à 512 caractères") String statement,
            @NotEmpty(message = "Une question a des propositions")
            @Valid List<ChoiceDraftRequest> choices) {

        CourseDraft.QuestionDraft toDraft() {
            return new CourseDraft.QuestionDraft(statement,
                    choices.stream().map(ChoiceDraftRequest::toDraft).toList());
        }
    }

    public record ChoiceDraftRequest(
            @NotBlank(message = "Une proposition ne peut pas être vide")
            @Size(max = 256, message = "Une proposition est limitée à 256 caractères") String label,
            boolean correct) {

        CourseDraft.ChoiceDraft toDraft() {
            return new CourseDraft.ChoiceDraft(label, correct);
        }
    }

    /** Machine du catalogue saisie dans l'éditeur. */
    public record BoxDraftRequest(
            @NotBlank(message = "Le nom est obligatoire")
            @Size(max = 64, message = "Le nom est limité à 64 caractères") String name,
            @NotNull(message = "Le système est obligatoire") OperatingSystem operatingSystem,
            @NotNull(message = "La difficulté est obligatoire") Difficulty difficulty,
            @Size(max = 512, message = "Le synopsis est limité à 512 caractères") String synopsis,
            @NotBlank(message = "L'adresse est obligatoire")
            @Size(max = 45, message = "Adresse trop longue") String ipAddress,
            @Size(max = 64, message = "Nom d'auteur trop long") String maker,
            boolean retired,
            /** Réservée aux abonnés Pro. Le formulaire l'envoie toujours. */
            boolean proOnly,
            String userFlag,
            String rootFlag) {

        public BoxDraft toDraft() {
            return new BoxDraft(name, operatingSystem, difficulty, synopsis, ipAddress, maker, retired, proOnly,
                    userFlag, rootFlag);
        }
    }

    /**
     * Machine publiée. Les flags en clair n'apparaissent que dans la réponse
     * qui suit leur tirage, pour être déposés sur la cible ; ils ne sont
     * conservés nulle part et ne seront plus jamais lisibles.
     */
    public record AdminBoxResponse(String slug, String name, String os, String osName, String difficulty,
                                   String difficultyName, int userFlagPoints, int rootFlagPoints, int totalPoints,
                                   String synopsis, String ipAddress, String maker, Instant releasedAt,
                                   boolean retired, boolean proOnly, String userFlagOnce, String rootFlagOnce) {

        public static AdminBoxResponse from(PublishedBox published) {
            return from(published.box(), published.userFlagOnce(), published.rootFlagOnce());
        }

        public static AdminBoxResponse from(Box box) {
            return from(box, null, null);
        }

        private static AdminBoxResponse from(Box box, String userFlagOnce, String rootFlagOnce) {
            return new AdminBoxResponse(box.getSlug(), box.getName(), box.getOperatingSystem().name(),
                    Texts.of(box.getOperatingSystem().displayName()), box.getDifficulty().name(),
                    Texts.of(box.getDifficulty().displayName()), box.pointsFor(FlagKind.USER), box.pointsFor(FlagKind.ROOT),
                    box.totalPoints(), box.getSynopsis(), box.getIpAddress(), box.getMaker(), box.getReleasedAt(),
                    box.isRetired(), box.isProOnly(), userFlagOnce, rootFlagOnce);
        }
    }

    public record AdminOverviewResponse(long users, long boxes, long courses, long sections, long flagsValidated,
                                        long sectionsCompleted) {

        public static AdminOverviewResponse from(AdminOverview overview) {
            return new AdminOverviewResponse(overview.users(), overview.boxes(), overview.courses(),
                    overview.sections(), overview.flagsValidated(), overview.sectionsCompleted());
        }
    }
}
