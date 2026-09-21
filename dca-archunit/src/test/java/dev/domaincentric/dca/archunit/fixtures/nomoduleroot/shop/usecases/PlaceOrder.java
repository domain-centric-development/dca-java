package dev.domaincentric.dca.archunit.fixtures.nomoduleroot.shop.usecases;

import dev.domaincentric.dca.archunit.fixtures.nomoduleroot.shop.core.Order;

/** A use case in a layer the layout does not know by name. */
public final class PlaceOrder {
  public Order execute(String id) {
    return new Order(id);
  }
}
