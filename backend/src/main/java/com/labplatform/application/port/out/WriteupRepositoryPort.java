package com.labplatform.application.port.out;

import com.labplatform.domain.writeup.Writeup;

import java.util.List;
import java.util.Optional;

public interface WriteupRepositoryPort {

    /** Comptes rendus d'une machine, tous auteurs confondus. Le filtrage de lecture est fait au-dessus. */
    List<Writeup> findByBox(Long boxId);

    Optional<Writeup> find(Long authorId, Long boxId);

    Writeup save(Writeup writeup);

    void delete(Long authorId, Long boxId);
}
