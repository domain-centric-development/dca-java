package dev.domaincentric.dca.archunit.fixtures.tactical.primitives.good.ledger.domain.event;

import dev.domaincentric.dca.archunit.fixtures.tactical.primitives.good.ledger.domain.model.AccountId;
import dev.domaincentric.dca.archunit.fixtures.tactical.primitives.good.ledger.domain.model.Holder;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record AccountOpened(UUID eventId, AccountId account, Holder holder, Instant occurredOn)
    implements DomainEvent {}
