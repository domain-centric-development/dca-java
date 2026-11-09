package dev.domaincentric.dca.archunit.fixtures.hexagonal.good.order.adapter.outgoing;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.good.order.domain.model.Order;

/** An outgoing adapter that only reads what the aggregate holds. */
public class OrderTotalExport {

  public String export(Order order) {
    return order.id() + ":" + order.total();
  }
}
