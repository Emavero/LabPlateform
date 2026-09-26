package com.labplatform.application.fakes;

import com.labplatform.application.port.out.WriteupRepositoryPort;
import com.labplatform.domain.writeup.Writeup;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryWriteups implements WriteupRepositoryPort {

    private final Map<String, Writeup> store = new LinkedHashMap<>();
    private long sequence = 0;

    @Override
    public List<Writeup> findByBox(Long boxId) {
        return store.values().stream().filter(writeup -> writeup.getBoxId().equals(boxId)).toList();
    }

    @Override
    public Optional<Writeup> find(Long authorId, Long boxId) {
        return Optional.ofNullable(store.get(key(authorId, boxId)));
    }

    /** Un seul compte rendu par couple (auteur, machine), comme en base. */
    @Override
    public Writeup save(Writeup writeup) {
        String key = key(writeup.getAuthorId(), writeup.getBoxId());
        Long id = writeup.getId() != null ? writeup.getId()
                : Optional.ofNullable(store.get(key)).map(Writeup::getId).orElse(++sequence);
        Writeup stored = Writeup.restore(id, writeup.getBoxId(), writeup.getAuthorId(), writeup.getTitle(),
                writeup.getContent(), writeup.isPublished(), writeup.getCreatedAt(), writeup.getUpdatedAt());
        store.put(key, stored);
        return stored;
    }

    @Override
    public void delete(Long authorId, Long boxId) {
        store.remove(key(authorId, boxId));
    }

    private static String key(Long authorId, Long boxId) {
        return authorId + ":" + boxId;
    }
}
