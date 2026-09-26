package com.labplatform.adapter.out.persistence;

import com.labplatform.adapter.out.persistence.entity.WriteupJpaEntity;
import com.labplatform.adapter.out.persistence.repository.SpringDataWriteupRepository;
import com.labplatform.application.port.out.WriteupRepositoryPort;
import com.labplatform.domain.writeup.Writeup;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class WriteupPersistenceAdapter implements WriteupRepositoryPort {

    private final SpringDataWriteupRepository repository;

    public WriteupPersistenceAdapter(SpringDataWriteupRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Writeup> findByBox(Long boxId) {
        return repository.findByBoxId(boxId).stream().map(WriteupPersistenceAdapter::toDomain).toList();
    }

    @Override
    public Optional<Writeup> find(Long authorId, Long boxId) {
        return repository.findByAuthorIdAndBoxId(authorId, boxId).map(WriteupPersistenceAdapter::toDomain);
    }

    @Override
    public Writeup save(Writeup writeup) {
        return toDomain(repository.save(new WriteupJpaEntity(writeup.getId(), writeup.getBoxId(),
                writeup.getAuthorId(), writeup.getTitle(), writeup.getContent(), writeup.isPublished(),
                writeup.getCreatedAt(), writeup.getUpdatedAt())));
    }

    @Override
    public void delete(Long authorId, Long boxId) {
        repository.deleteByAuthorIdAndBoxId(authorId, boxId);
    }

    private static Writeup toDomain(WriteupJpaEntity e) {
        return Writeup.restore(e.getId(), e.getBoxId(), e.getAuthorId(), e.getTitle(), e.getContent(),
                e.isPublished(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
