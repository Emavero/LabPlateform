package com.labplatform.adapter.out.transaction;

import com.labplatform.application.port.out.TransactionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/** Traduit la frontière transactionnelle de l'application en transaction Spring (propagation REQUIRED). */
@Component
public class SpringTransactionAdapter implements TransactionPort {

    private final TransactionTemplate template;

    public SpringTransactionAdapter(PlatformTransactionManager transactionManager) {
        this.template = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        return template.execute(status -> work.get());
    }
}
