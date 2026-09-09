package dev.domaincentric.dca.archunit.fixtures.frameworks.micronaut.billing.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record InvoiceIssued(UUID eventId, Instant occurredOn, InvoiceId invoiceId)
    implements DomainEvent {}
