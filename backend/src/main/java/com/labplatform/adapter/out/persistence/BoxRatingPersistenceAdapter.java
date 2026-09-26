package com.labplatform.adapter.out.persistence;

import com.labplatform.adapter.out.persistence.entity.BoxRatingJpaEntity;
import com.labplatform.adapter.out.persistence.repository.SpringDataBoxRatingRepository;
import com.labplatform.application.port.out.BoxRatingRepositoryPort;
import com.labplatform.domain.box.BoxRating;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class BoxRatingPersistenceAdapter implements BoxRatingRepositoryPort {

    private final SpringDataBoxRatingRepository repository;

    public BoxRatingPersistenceAdapter(SpringDataBoxRatingRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<BoxRating> find(Long userId, Long boxId) {
        return repository.findByUserIdAndBoxId(userId, boxId).map(BoxRatingPersistenceAdapter::toDomain);
    }

    /** Un joueur n'a qu'un vote par machine : revoter réécrit la même ligne. */
    @Override
    public BoxRating save(BoxRating rating) {
        Long id = rating.id() != null
                ? rating.id()
                : repository.findByUserIdAndBoxId(rating.userId(), rating.boxId())
                        .map(BoxRatingJpaEntity::getId)
                        .orElse(null);
        return toDomain(repository.save(new BoxRatingJpaEntity(id, rating.userId(), rating.boxId(),
                rating.difficulty(), rating.ratedAt())));
    }

    @Override
    public List<Tally> tallies() {
        return repository.tallies();
    }

    private static BoxRating toDomain(BoxRatingJpaEntity e) {
        return BoxRating.restore(e.getId(), e.getUserId(), e.getBoxId(), e.getDifficulty(), e.getRatedAt());
    }
}
