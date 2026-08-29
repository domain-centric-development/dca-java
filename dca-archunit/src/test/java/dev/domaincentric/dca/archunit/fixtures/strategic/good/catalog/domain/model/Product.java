package dev.domaincentric.dca.archunit.fixtures.strategic.good.catalog.domain.model;

import dev.domaincentric.dca.archunit.fixtures.strategic.good.sharedkernel.domain.model.Money;

public final class Product {
  private final String name;
  private final Money price;

  public Product(String name, Money price) {
    this.name = name;
    this.price = price;
  }

  public String name() {
    return name;
  }

  public Money price() {
    return price;
  }
}
