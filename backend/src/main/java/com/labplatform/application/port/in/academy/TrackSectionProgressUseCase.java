package com.labplatform.application.port.in.academy;

import com.labplatform.domain.user.Actor;

/**
 * Suivi de lecture. Les deux opérations sont idempotentes : cocher une
 * section déjà terminée, ou en rouvrir une qui ne l'est pas, laisse
 * l'avancement inchangé plutôt que de refuser.
 */
public interface TrackSectionProgressUseCase {

    CourseView completeSection(Actor actor, String courseSlug, String sectionSlug);

    CourseView reopenSection(Actor actor, String courseSlug, String sectionSlug);
}
