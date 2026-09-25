package com.labplatform.application.fakes;

import com.labplatform.application.port.out.BoxRepositoryPort;
import com.labplatform.domain.box.Box;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryBoxes implements BoxRepositoryPort {

    private final Map<Long, Box> store = new LinkedHashMap<>();
    private long sequence = 0;

    @Override
    public List<Box> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public Optional<Box> findBySlug(String slug) {
        return store.values().stream().filter(box -> box.getSlug().equals(slug)).findFirst();
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public Box save(Box box) {
        Long id = box.getId() != null ? box.getId() : ++sequence;
        Box stored = Box.restore(id, box.getSlug(), box.getName(), box.getOperatingSystem(), box.getDifficulty(),
                box.getSynopsis(), box.getIpAddress(), box.getMaker(), box.getReleasedAt(), box.isRetired(),
                box.getUserFlag(), box.getRootFlag());
        store.put(id, stored);
        return stored;
    }
}
