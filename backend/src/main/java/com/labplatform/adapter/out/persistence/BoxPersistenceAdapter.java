package com.labplatform.adapter.out.persistence;

import com.labplatform.adapter.out.persistence.entity.BoxJpaEntity;
import com.labplatform.adapter.out.persistence.repository.SpringDataBoxRepository;
import com.labplatform.application.port.out.BoxRepositoryPort;
import com.labplatform.domain.box.Box;
import com.labplatform.domain.box.Flag;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class BoxPersistenceAdapter implements BoxRepositoryPort {

    private final SpringDataBoxRepository repository;

    public BoxPersistenceAdapter(SpringDataBoxRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Box> findAll() {
        return repository.findAll().stream().map(BoxPersistenceAdapter::toDomain).toList();
    }

    @Override
    public Optional<Box> findBySlug(String slug) {
        return repository.findBySlug(slug).map(BoxPersistenceAdapter::toDomain);
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public Box save(Box box) {
        return toDomain(repository.save(toEntity(box)));
    }

    private static BoxJpaEntity toEntity(Box box) {
        return new BoxJpaEntity(box.getId(), box.getSlug(), box.getName(), box.getOperatingSystem(),
                box.getDifficulty(), box.getSynopsis(), box.getIpAddress(), box.getMaker(), box.getReleasedAt(),
                box.isRetired(), box.getUserFlag().hash(), box.getRootFlag().hash());
    }

    private static Box toDomain(BoxJpaEntity e) {
        return Box.restore(e.getId(), e.getSlug(), e.getName(), e.getOperatingSystem(), e.getDifficulty(),
                e.getSynopsis(), e.getIpAddress(), e.getMaker(), e.getReleasedAt(), e.isRetired(),
                Flag.ofHash(e.getUserFlagHash()), Flag.ofHash(e.getRootFlagHash()));
    }
}
