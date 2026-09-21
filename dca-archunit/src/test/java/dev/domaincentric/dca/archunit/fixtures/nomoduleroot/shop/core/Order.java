package dev.domaincentric.dca.archunit.fixtures.nomoduleroot.shop.core;

/** A model type in a layer the layout does not know by name. */
public final class Order {
  private final String id;

  public Order(String id) {
    this.id = id;
  }

  public String id() {
    return id;
  }
}
