package com.labplatform.application.port.out;

import java.util.function.Supplier;

/**
 * Frontière transactionnelle exprimée sans framework : l'application
 * décide de ce qui doit être atomique, l'adaptateur décide comment.
 */
public interface TransactionPort {

    <T> T inTransaction(Supplier<T> work);

    default void inTransaction(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }
}
