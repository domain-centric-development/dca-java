package dev.domaincentric.dca.archunit.fixtures.strategic.bad.sharedkernel.domain.model;

import dev.domaincentric.dca.archunit.fixtures.strategic.bad.catalog.domain.model.Product;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

public record Money(long cents) implements Value {
  public static Money of(Product product) {
    return new Money(0);
  }
}
