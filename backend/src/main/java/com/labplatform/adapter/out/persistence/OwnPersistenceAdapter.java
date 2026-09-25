package com.labplatform.adapter.out.persistence;

import com.labplatform.adapter.out.persistence.entity.BoxOwnJpaEntity;
import com.labplatform.adapter.out.persistence.repository.SpringDataBoxOwnRepository;
import com.labplatform.application.port.out.OwnRepositoryPort;
import com.labplatform.domain.box.FlagKind;
import com.labplatform.domain.box.Own;
import com.labplatform.domain.scoring.PlayerScore;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OwnPersistenceAdapter implements OwnRepositoryPort {

    private final SpringDataBoxOwnRepository repository;

    public OwnPersistenceAdapter(SpringDataBoxOwnRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Own> findByUser(Long userId) {
        return repository.findByUserId(userId).stream().map(OwnPersistenceAdapter::toDomain).toList();
    }

    @Override
    public boolean exists(Long userId, Long boxId, FlagKind kind) {
        return repository.existsByUserIdAndBoxIdAndKind(userId, boxId, kind);
    }

    @Override
    public boolean noneYet(Long boxId, FlagKind kind) {
        return !repository.existsByBoxIdAndKind(boxId, kind);
    }

    @Override
    public Own save(Own own) {
        return toDomain(repository.save(toEntity(own)));
    }

    @Override
    public List<PlayerScore> topScores(int limit) {
        return repository.topScores(PageRequest.of(0, limit));
    }

    private static BoxOwnJpaEntity toEntity(Own own) {
        return new BoxOwnJpaEntity(own.id(), own.userId(), own.boxId(), own.kind(), own.points(), own.firstBlood(),
                own.ownedAt());
    }

    private static Own toDomain(BoxOwnJpaEntity e) {
        return Own.restore(e.getId(), e.getUserId(), e.getBoxId(), e.getKind(), e.getPoints(), e.isFirstBlood(),
                e.getOwnedAt());
    }
}
