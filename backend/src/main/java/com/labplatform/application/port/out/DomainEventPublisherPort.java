package com.labplatform.application.port.out;

public interface DomainEventPublisherPort {

    void publish(Object domainEvent);
}
