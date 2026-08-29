package dev.domaincentric.dca.archunit.fixtures.strategic.bad.catalog.domain.model;

public final class Product {
  private final String name;

  public Product(String name) {
    this.name = name;
  }

  public String name() {
    return name;
  }
}
