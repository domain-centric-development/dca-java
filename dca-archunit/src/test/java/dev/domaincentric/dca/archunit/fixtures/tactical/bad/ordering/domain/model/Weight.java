package dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

/**
 * DCA-TAC-012: {@code equals(Weight)} is an overload, not an override — instances are still
 * compared by identity through the inherited {@code Object.equals(Object)}.
 */
public final class Weight implements Value {
  private final int grams;

  public Weight(int grams) {
    this.grams = grams;
  }

  public boolean equals(Weight other) {
    return other != null && other.grams == grams;
  }

  @Override
  public int hashCode() {
    return grams;
  }
}
