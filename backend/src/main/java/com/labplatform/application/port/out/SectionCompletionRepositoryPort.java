package com.labplatform.application.port.out;

import com.labplatform.domain.academy.SectionCompletion;

import java.util.List;

public interface SectionCompletionRepositoryPort {

    List<SectionCompletion> findByUser(Long userId);

    SectionCompletion save(SectionCompletion completion);

    /** Sans effet si la section n'était pas cochée. */
    void delete(Long userId, Long sectionId);

    boolean exists(Long userId, Long sectionId);
}
