package dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.domain.model;

public class Entry
    extends dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot<Entry, EntryId> {
  private final EntryId id;

  public Entry(EntryId id) {
    this.id = id;
  }

  public EntryId id() {
    return id;
  }
}
