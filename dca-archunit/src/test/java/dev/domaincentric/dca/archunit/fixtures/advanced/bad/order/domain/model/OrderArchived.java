package dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record OrderArchived(UUID eventId) implements DomainEvent {
  @Override
  public Instant occurredOn() {
    return Instant.EPOCH;
  }
}
