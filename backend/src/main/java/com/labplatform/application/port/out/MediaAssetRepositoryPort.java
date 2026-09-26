package com.labplatform.application.port.out;

import com.labplatform.domain.media.MediaAsset;

import java.util.Optional;

public interface MediaAssetRepositoryPort {

    Optional<MediaAsset> findById(String id);

    MediaAsset save(MediaAsset asset);

    void delete(String id);
}
