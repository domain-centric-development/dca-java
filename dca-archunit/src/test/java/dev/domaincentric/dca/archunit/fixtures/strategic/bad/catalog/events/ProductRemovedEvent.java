package dev.domaincentric.dca.archunit.fixtures.strategic.bad.catalog.events;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.IntegrationEvent;
import java.time.Instant;
import java.util.UUID;

public final class ProductRemovedEvent implements IntegrationEvent {
  @Override
  public UUID eventId() {
    return UUID.randomUUID();
  }

  @Override
  public Instant occurredOn() {
    return Instant.now();
  }
}
