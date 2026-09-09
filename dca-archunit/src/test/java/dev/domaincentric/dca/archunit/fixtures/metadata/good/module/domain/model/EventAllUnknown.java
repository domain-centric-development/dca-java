package dev.domaincentric.dca.archunit.fixtures.metadata.good.module.domain.model;

import dev.domaincentric.dca.archunit.fixtures.metadata.annotations.*;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.*;

@Unknown
public final class EventAllUnknown implements DomainEvent {
  @Unknown private int value;

  @Unknown
  public EventAllUnknown() {}

  @Unknown
  public void operation() {}

  public java.util.UUID eventId() {
    return java.util.UUID.randomUUID();
  }

  public java.time.Instant occurredOn() {
    return java.time.Instant.EPOCH;
  }
}
