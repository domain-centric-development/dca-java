package dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.getorder;

import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.domain.model.Money;
import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.domain.model.OrderId;
import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.domain.readmodel.OrderSnapshot;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public final class GetOrderUseCase implements GetOrderInputPort {
  private final OrderRepository orders;

  public GetOrderUseCase(OrderRepository orders) {
    this.orders = orders;
  }

  @Override
  public GetOrderResult execute(GetOrderQuery query) {
    OrderId id = new OrderId(query.orderId());
    Money total = new Money(0, "EUR");
    return new GetOrderResult(
        id,
        total,
        orders.findById(id).map(order -> new OrderSnapshot(order.id(), total)),
        List.of(),
        Map.of(),
        List.of(),
        new GetOrderResult.Customer("", ""));
  }
}
