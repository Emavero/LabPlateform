package com.labplatform.application.fakes;

import com.labplatform.application.port.out.SectionCompletionRepositoryPort;
import com.labplatform.domain.academy.SectionCompletion;

import java.util.ArrayList;
import java.util.List;

public class InMemoryCompletions implements SectionCompletionRepositoryPort {

    private final List<SectionCompletion> store = new ArrayList<>();
    private long sequence = 0;

    @Override
    public List<SectionCompletion> findByUser(Long userId) {
        return store.stream().filter(done -> done.userId().equals(userId)).toList();
    }

    @Override
    public SectionCompletion save(SectionCompletion completion) {
        SectionCompletion stored = SectionCompletion.restore(++sequence, completion.userId(), completion.courseId(),
                completion.sectionId(), completion.completedAt());
        store.add(stored);
        return stored;
    }

    @Override
    public void delete(Long userId, Long sectionId) {
        store.removeIf(done -> done.userId().equals(userId) && done.sectionId().equals(sectionId));
    }

    @Override
    public boolean exists(Long userId, Long sectionId) {
        return store.stream().anyMatch(done -> done.userId().equals(userId) && done.sectionId().equals(sectionId));
    }

    public int count() {
        return store.size();
    }
}
