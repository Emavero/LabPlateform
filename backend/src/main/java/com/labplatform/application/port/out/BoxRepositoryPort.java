package com.labplatform.application.port.out;

import com.labplatform.domain.box.Box;

import java.util.List;
import java.util.Optional;

public interface BoxRepositoryPort {

    List<Box> findAll();

    Optional<Box> findBySlug(String slug);

    long count();

    Box save(Box box);
}
