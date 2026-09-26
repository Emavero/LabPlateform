package com.labplatform.adapter.out.persistence;

import com.labplatform.adapter.out.persistence.entity.SectionCompletionJpaEntity;
import com.labplatform.adapter.out.persistence.repository.SpringDataSectionCompletionRepository;
import com.labplatform.application.port.out.SectionCompletionRepositoryPort;
import com.labplatform.domain.academy.SectionCompletion;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SectionCompletionPersistenceAdapter implements SectionCompletionRepositoryPort {

    private final SpringDataSectionCompletionRepository repository;

    public SectionCompletionPersistenceAdapter(SpringDataSectionCompletionRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<SectionCompletion> findByUser(Long userId) {
        return repository.findByUserId(userId).stream().map(SectionCompletionPersistenceAdapter::toDomain).toList();
    }

    @Override
    public SectionCompletion save(SectionCompletion completion) {
        return toDomain(repository.save(new SectionCompletionJpaEntity(completion.id(), completion.userId(),
                completion.courseId(), completion.sectionId(), completion.completedAt())));
    }

    @Override
    public void delete(Long userId, Long sectionId) {
        repository.deleteByUserIdAndSectionId(userId, sectionId);
    }

    @Override
    public boolean exists(Long userId, Long sectionId) {
        return repository.existsByUserIdAndSectionId(userId, sectionId);
    }

    private static SectionCompletion toDomain(SectionCompletionJpaEntity e) {
        return SectionCompletion.restore(e.getId(), e.getUserId(), e.getCourseId(), e.getSectionId(),
                e.getCompletedAt());
    }
}
