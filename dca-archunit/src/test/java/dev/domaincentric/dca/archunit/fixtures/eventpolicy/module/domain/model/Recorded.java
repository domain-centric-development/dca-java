package dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.domain.model;

public class Recorded
    extends dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot<Recorded, EntryId> {
  public EntryId id() {
    return new EntryId(java.util.UUID.randomUUID());
  }

  public void change() {
    recordChange();
  }

  private void recordChange() {
    registerEvent(new Changed(java.util.UUID.randomUUID(), java.time.Instant.EPOCH, 1));
  }
}
