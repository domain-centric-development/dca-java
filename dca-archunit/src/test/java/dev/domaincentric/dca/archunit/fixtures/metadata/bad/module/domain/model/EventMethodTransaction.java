package dev.domaincentric.dca.archunit.fixtures.metadata.bad.module.domain.model;

import dev.domaincentric.dca.archunit.fixtures.metadata.annotations.*;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.*;

public final class EventMethodTransaction implements DomainEvent {
  private int value;

  public EventMethodTransaction() {}

  @Transaction
  public void operation() {}

  public java.util.UUID eventId() {
    return java.util.UUID.randomUUID();
  }

  public java.time.Instant occurredOn() {
    return java.time.Instant.EPOCH;
  }
}
