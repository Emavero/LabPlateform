package com.labplatform.adapter.out.event;

import com.labplatform.application.port.out.DomainEventPublisherPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Relaie les événements de domaine sur le bus d'événements Spring.
 * La publication est synchrone : les écouteurs s'exécutent dans la
 * transaction de l'appelant.
 */
@Component
public class SpringDomainEventPublisher implements DomainEventPublisherPort {

    private final ApplicationEventPublisher publisher;

    public SpringDomainEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(Object domainEvent) {
        publisher.publishEvent(domainEvent);
    }
}
