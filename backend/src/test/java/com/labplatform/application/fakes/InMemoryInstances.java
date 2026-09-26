package com.labplatform.application.fakes;

import com.labplatform.application.port.out.BoxInstanceRepositoryPort;
import com.labplatform.domain.box.BoxInstance;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryInstances implements BoxInstanceRepositoryPort {

    private final Map<String, BoxInstance> store = new LinkedHashMap<>();
    private long sequence = 0;

    @Override
    public List<BoxInstance> findByUser(Long userId) {
        return store.values().stream().filter(instance -> instance.getUserId().equals(userId)).toList();
    }

    @Override
    public Optional<BoxInstance> find(Long userId, Long boxId) {
        return Optional.ofNullable(store.get(key(userId, boxId)));
    }

    @Override
    public Optional<BoxInstance> findRunningByUser(Long userId) {
        return findByUser(userId).stream().filter(BoxInstance::isRunning).findFirst();
    }

    /** Une seule ligne par couple (joueur, machine), comme la contrainte d'unicité en base. */
    @Override
    public BoxInstance save(BoxInstance instance) {
        String key = key(instance.getUserId(), instance.getBoxId());
        Long id = instance.getId() != null ? instance.getId()
                : Optional.ofNullable(store.get(key)).map(BoxInstance::getId).orElse(++sequence);
        BoxInstance stored = BoxInstance.restore(id, instance.getUserId(), instance.getBoxId(),
                instance.getStatus(), instance.getAddress().orElse(null), instance.getStartedAt().orElse(null),
                instance.getExpiresAt().orElse(null));
        store.put(key, stored);
        return stored;
    }

    private static String key(Long userId, Long boxId) {
        return userId + ":" + boxId;
    }
}
