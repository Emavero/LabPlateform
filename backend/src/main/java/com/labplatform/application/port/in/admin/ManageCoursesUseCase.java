package com.labplatform.application.port.in.admin;

import com.labplatform.domain.academy.Course;
import com.labplatform.domain.user.Actor;

public interface ManageCoursesUseCase {

    /**
     * Publie un nouveau cours. Son identifiant d'URL est dérivé du titre.
     *
     * @throws com.labplatform.domain.shared.ForbiddenException appelant non administrateur
     * @throws com.labplatform.domain.shared.ConflictException  un cours porte déjà ce titre
     */
    Course createCourse(Actor actor, CourseDraft draft);

    /** Remplace le contenu d'un cours. Son identifiant d'URL ne change pas. */
    Course updateCourse(Actor actor, String slug, CourseDraft draft);

    /** Supprime un cours, ses sections et l'avancement qui s'y rapporte. */
    void deleteCourse(Actor actor, String slug);
}
