package dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.getorder;

import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.shared.OrderPart;
import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.domain.model.Money;
import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.domain.model.OrderId;
import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.domain.readmodel.OrderSnapshot;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// DCA-USE-015: values only - an Id, Money, a Value read model, nested and shared part records and
// generic arguments over values.
public record GetOrderResult(
    OrderId orderId,
    Money total,
    Optional<OrderSnapshot> snapshot,
    List<OrderLine> lines,
    Map<String, Money> discounts,
    List<OrderPart> parts,
    Customer customer) {

  /** Nested part record of primitives. */
  public record Customer(String name, String email) {}
}
