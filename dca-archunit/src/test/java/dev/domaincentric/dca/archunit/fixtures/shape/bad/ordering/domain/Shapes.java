package dev.domaincentric.dca.archunit.fixtures.shape.bad.ordering.domain;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.IntegrationEvent;
import java.time.Instant;
import java.util.UUID;

public final class Shapes {
  public static final class StateChanged
      implements IntegrationEvent, dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainEvent {
    private int amount = 1;

    public UUID eventId() {
      return new UUID(0, 0);
    }

    public Instant occurredOn() {
      return Instant.EPOCH;
    }
  }
}
