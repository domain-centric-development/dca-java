package dev.domaincentric.dca.archunit.fixtures.usecase.readonly.bad.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record AccountOpened(UUID eventId, Instant occurredOn, AccountId accountId)
    implements DomainEvent {}
