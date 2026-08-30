package dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.shiporder;

import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.shared.CarrierPort;
import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.UnitOfWork;
import org.springframework.stereotype.Service;

// DCA-USE-012/013: remote call first, then an explicit UnitOfWork boundary around save + publish
@Service
public final class ShipOrderUseCase implements ShipOrderInputPort {
  private final OrderRepository orders;
  private final CarrierPort carrier;
  private final DomainEventPublisher events;
  private final UnitOfWork unitOfWork;

  public ShipOrderUseCase(
      OrderRepository orders,
      CarrierPort carrier,
      DomainEventPublisher events,
      UnitOfWork unitOfWork) {
    this.orders = orders;
    this.carrier = carrier;
    this.events = events;
    this.unitOfWork = unitOfWork;
  }

  @Override
  public ShipOrderResult execute(ShipOrderCommand command) {
    OrderId id = new OrderId(command.orderId());
    String quote = carrier.quote(id);
    return unitOfWork.run(
        () -> {
          Order order = orders.findById(id).orElseThrow();
          orders.save(order);
          events.publishAndClearEvents(order);
          return new ShipOrderResult(order.id().value(), quote);
        });
  }
}
