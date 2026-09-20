package dev.domaincentric.dca.archunit.fixtures.strategic.undeclared.ordering.domain.model;

/** A module that owns a domain layer while no package below the base package declares a context. */
public final class Order {

  private final String id;

  public Order(String id) {
    this.id = id;
  }

  public String id() {
    return id;
  }
}
