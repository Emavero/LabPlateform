package com.labplatform.application.fakes;

import com.labplatform.application.port.out.MediaAssetRepositoryPort;
import com.labplatform.application.port.out.MediaStoragePort;
import com.labplatform.domain.media.MediaAsset;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Stockage et fiches en mémoire : le contenu n'atteint jamais un disque. */
public class InMemoryMedia implements MediaStoragePort, MediaAssetRepositoryPort {

    private final Map<String, byte[]> contents = new LinkedHashMap<>();
    private final Map<String, MediaAsset> assets = new LinkedHashMap<>();

    @Override
    public long write(String id, InputStream content) {
        try {
            byte[] bytes = content.readAllBytes();
            contents.put(id, bytes);
            return bytes.length;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Optional<StoredContent> read(String id) {
        return Optional.ofNullable(contents.get(id))
                .map(bytes -> new StoredContent("/mémoire/" + id, bytes.length));
    }

    @Override
    public void delete(String id) {
        contents.remove(id);
        assets.remove(id);
    }

    @Override
    public Optional<MediaAsset> findById(String id) {
        return Optional.ofNullable(assets.get(id));
    }

    @Override
    public MediaAsset save(MediaAsset asset) {
        assets.put(asset.id(), asset);
        return asset;
    }

    public boolean holds(String id) {
        return contents.containsKey(id);
    }

    public int count() {
        return assets.size();
    }
}
