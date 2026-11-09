package dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.adapter.outgoing;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.domain.model.Order;

// Violates HEX-021: an outgoing adapter that changes the aggregate it exports. The queries it
// asks - isArchived(), total() - pass, and so does clearing the domain events the building
// blocks' aggregate base keeps.
public class OrderArchiveExport {

  public String export(Order order) {
    if (!order.isArchived()) {
      order.archive();
    }
    order.clearDomainEvents();
    return order.id() + ":" + order.total();
  }
}
