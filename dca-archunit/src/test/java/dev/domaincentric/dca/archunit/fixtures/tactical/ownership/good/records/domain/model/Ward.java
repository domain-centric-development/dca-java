package dev.domaincentric.dca.archunit.fixtures.tactical.ownership.good.records.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;

/** Belongs to nobody: its repository needs no scoped lookup. */
public final class Ward extends BaseAggregateRoot<Ward, WardId> {
  private final WardId id;

  private Ward(WardId id) {
    this.id = id;
  }

  public static Ward open(WardId id) {
    return new Ward(id);
  }

  @Override
  public WardId id() {
    return id;
  }
}
