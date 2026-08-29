package dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.domain.model;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.infrastructure.config.InMemoryConfig;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.adapter.outgoing.InMemoryOrderRepository;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.application.placeorder.PlaceOrderCommand;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.sharedkernel.domain.model.Money;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// Violates HEX-001 (adapter dependency), LAY-002 (infrastructure dependency), ONI-001
// (application dependency), ONI-002 (Spring dependency), ONI-003 (@Component), LAY-004
// (@Transactional in the domain).
@Component
public class Order extends BaseAggregateRoot<Order, OrderId> {
  private final OrderId id;
  private final Money total;
  private final InMemoryOrderRepository repository = new InMemoryOrderRepository();

  public Order(OrderId id, Money total) {
    this.id = id;
    this.total = total;
    registerEvent(new OrderPlaced(UUID.randomUUID(), Instant.now(), id));
  }

  public static Order from(PlaceOrderCommand command) {
    return new Order(new OrderId("x"), new Money(command.cents() + InMemoryConfig.capacity()));
  }

  @Transactional
  public void persist() {
    repository.save(this);
  }

  @Override
  public OrderId id() {
    return id;
  }

  public Money total() {
    return total;
  }
}
