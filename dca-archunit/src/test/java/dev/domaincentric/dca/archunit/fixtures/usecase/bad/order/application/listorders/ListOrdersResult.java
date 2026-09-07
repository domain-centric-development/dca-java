package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.listorders;

import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.shared.OrderPart;
import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.domain.model.Order;
import java.util.List;

// DCA-USE-015: List<Order> hands out the aggregate; the nested part record hides another one; the
// same-package and the application.shared part records hide an entity; the same part record
// appears twice, and both paths are reported.
public record ListOrdersResult(
    List<Order> orders,
    Highlight highlight,
    OrderLine firstLine,
    OrderLine lastLine,
    List<OrderPart> parts) {

  public record Highlight(Order order) {}
}
