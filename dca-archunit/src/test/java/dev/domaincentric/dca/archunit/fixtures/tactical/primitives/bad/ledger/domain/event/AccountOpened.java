package dev.domaincentric.dca.archunit.fixtures.tactical.primitives.bad.ledger.domain.event;

import dev.domaincentric.dca.archunit.fixtures.tactical.primitives.bad.ledger.domain.model.AccountId;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainEvent;
import java.time.Instant;
import java.util.UUID;

/** occurredOn is the event marker's contract and passes; the holder's name does not. */
public record AccountOpened(UUID eventId, AccountId account, String holder, Instant occurredOn)
    implements DomainEvent {}
