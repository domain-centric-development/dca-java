package dev.domaincentric.dca.archunit.fixtures.tactical.ownership.bad.records.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Owned;

/** Owned, but raw: it names no owner id type. */
@SuppressWarnings("rawtypes")
public final class Consent extends BaseAggregateRoot<Consent, RecordId> implements Owned {
  private final RecordId id;

  private Consent(RecordId id) {
    this.id = id;
  }

  public static Consent give(RecordId id) {
    return new Consent(id);
  }

  @Override
  public RecordId id() {
    return id;
  }
}
