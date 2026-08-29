package dev.domaincentric.dca.buildingblocks.ddd.tactical;

public interface Entity<T extends Entity<T, ID>, ID extends Id> {
  ID id();

  default boolean sameIdentityAs(final T other) {
    return other != null && id().equals(other.id());
  }
}
