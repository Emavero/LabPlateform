package com.labplatform.adapter.out.persistence;

import com.labplatform.adapter.out.persistence.entity.MediaAssetJpaEntity;
import com.labplatform.adapter.out.persistence.repository.SpringDataMediaAssetRepository;
import com.labplatform.application.port.out.MediaAssetRepositoryPort;
import com.labplatform.domain.media.MediaAsset;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MediaAssetPersistenceAdapter implements MediaAssetRepositoryPort {

    private final SpringDataMediaAssetRepository repository;

    public MediaAssetPersistenceAdapter(SpringDataMediaAssetRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<MediaAsset> findById(String id) {
        return repository.findById(id).map(MediaAssetPersistenceAdapter::toDomain);
    }

    @Override
    public MediaAsset save(MediaAsset asset) {
        return toDomain(repository.save(new MediaAssetJpaEntity(asset.id(), asset.filename(), asset.contentType(),
                asset.sizeBytes(), asset.uploadedAt(), asset.uploadedBy())));
    }

    @Override
    public void delete(String id) {
        repository.deleteById(id);
    }

    private static MediaAsset toDomain(MediaAssetJpaEntity e) {
        return new MediaAsset(e.getId(), e.getFilename(), e.getContentType(), e.getSizeBytes(), e.getUploadedAt(),
                e.getUploadedBy());
    }
}
