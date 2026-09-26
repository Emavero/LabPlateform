package com.labplatform.adapter.in.web.dto;

import com.labplatform.application.port.in.admin.AdminOverview;
import com.labplatform.application.port.in.admin.CourseDraft;
import com.labplatform.domain.academy.CourseLevel;
import com.labplatform.domain.academy.SectionKind;
import com.labplatform.domain.academy.Track;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** Représentations HTTP du tableau de bord d'administration. */
public final class AdminDtos {

    private AdminDtos() {
    }

    /** Cours saisi dans l'éditeur. */
    public record CourseDraftRequest(
            @NotBlank(message = "Le titre est obligatoire")
            @Size(max = 128, message = "Le titre est limité à 128 caractères") String title,
            @NotNull(message = "La filière est obligatoire") Track track,
            @NotNull(message = "Le niveau est obligatoire") CourseLevel level,
            @Size(max = 512, message = "Le résumé est limité à 512 caractères") String summary,
            @NotEmpty(message = "Un cours comporte au moins une section")
            @Valid List<SectionDraftRequest> sections) {

        public CourseDraft toDraft() {
            return new CourseDraft(title, track, level, summary,
                    sections.stream().map(SectionDraftRequest::toDraft).toList());
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
            @Size(max = 512, message = "L'adresse de la vidéo est trop longue") String videoUrl) {

        CourseDraft.SectionDraft toDraft() {
            return new CourseDraft.SectionDraft(id, title, kind, minutes, content, videoUrl);
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
