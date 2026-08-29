package dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public class OrderShippedClassEvent implements DomainEvent {
  private final UUID eventId = UUID.randomUUID();
  private final Instant occurredOn = Instant.now();

  @Override
  public UUID eventId() {
    return eventId;
  }

  @Override
  public Instant occurredOn() {
    return occurredOn;
  }
}
