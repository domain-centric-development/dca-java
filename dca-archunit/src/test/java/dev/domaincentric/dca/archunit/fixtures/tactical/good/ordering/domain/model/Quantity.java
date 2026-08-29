package dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;
import java.util.Objects;

/** A hand-written (non-record) value object: final class, final fields, attribute equality. */
public final class Quantity implements Value {
  private static int instances;
  private final int amount;

  public Quantity(int amount) {
    if (amount < 1) {
      throw new IllegalArgumentException("Quantity must be positive");
    }
    this.amount = amount;
    instances++;
  }

  public int amount() {
    return amount;
  }

  public Quantity plus(Quantity other) {
    return new Quantity(amount + other.amount);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Quantity && ((Quantity) o).amount == amount;
  }

  @Override
  public int hashCode() {
    return Objects.hash(amount);
  }
}
