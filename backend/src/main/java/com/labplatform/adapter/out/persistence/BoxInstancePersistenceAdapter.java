package com.labplatform.adapter.out.persistence;

import com.labplatform.adapter.out.persistence.entity.BoxInstanceJpaEntity;
import com.labplatform.adapter.out.persistence.repository.SpringDataBoxInstanceRepository;
import com.labplatform.application.port.out.BoxInstanceRepositoryPort;
import com.labplatform.domain.box.BoxInstance;
import com.labplatform.domain.lab.VmStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class BoxInstancePersistenceAdapter implements BoxInstanceRepositoryPort {

    private final SpringDataBoxInstanceRepository repository;

    public BoxInstancePersistenceAdapter(SpringDataBoxInstanceRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<BoxInstance> findByUser(Long userId) {
        return repository.findByUserId(userId).stream().map(BoxInstancePersistenceAdapter::toDomain).toList();
    }

    @Override
    public Optional<BoxInstance> find(Long userId, Long boxId) {
        return repository.findByUserIdAndBoxId(userId, boxId).map(BoxInstancePersistenceAdapter::toDomain);
    }

    @Override
    public Optional<BoxInstance> findRunningByUser(Long userId) {
        return repository.findFirstByUserIdAndStatus(userId, VmStatus.RUNNING)
                .map(BoxInstancePersistenceAdapter::toDomain);
    }

    @Override
    public BoxInstance save(BoxInstance instance) {
        Long id = instance.getId() != null
                ? instance.getId()
                : repository.findByUserIdAndBoxId(instance.getUserId(), instance.getBoxId())
                        .map(BoxInstanceJpaEntity::getId)
                        .orElse(null);
        return toDomain(repository.save(new BoxInstanceJpaEntity(id, instance.getUserId(), instance.getBoxId(),
                instance.getStatus(), instance.getAddress().orElse(null), instance.getStartedAt().orElse(null),
                instance.getExpiresAt().orElse(null))));
    }

    private static BoxInstance toDomain(BoxInstanceJpaEntity e) {
        return BoxInstance.restore(e.getId(), e.getUserId(), e.getBoxId(), e.getStatus(), e.getAddress(),
                e.getStartedAt(), e.getExpiresAt());
    }
}
