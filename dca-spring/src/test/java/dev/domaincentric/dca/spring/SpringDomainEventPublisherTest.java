package dev.domaincentric.dca.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainEvent;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class SpringDomainEventPublisherTest {

  private final List<Object> received = new ArrayList<>();
  private final ApplicationEventPublisher spring = received::add;

  @Test
  void publishesEveryCollectedEventAndClearsTheAggregateAfterwards() {
    var order = new Fixtures.Order();
    order.place();
    order.place();

    new SpringDomainEventPublisher(spring).publishAndClearEvents(order);

    assertEquals(2, received.size());
    assertTrue(order.domainEvents().isEmpty());
  }

  @Test
  void aThrowingListenerLeavesTheEventsOnTheAggregate() {
    var order = new Fixtures.Order();
    order.place();
    ApplicationEventPublisher failing =
        event -> {
          throw new IllegalStateException("listener failed");
        };

    var publisher = new SpringDomainEventPublisher(failing);
    assertThrows(IllegalStateException.class, () -> publisher.publishAndClearEvents(order));

    assertEquals(1, order.domainEvents().size(), "clearing is the acknowledgement — not given");
  }

  @Test
  void anAggregateWithoutEventsPublishesNothing() {
    new SpringDomainEventPublisher(spring).publishAndClearEvents(new Fixtures.Order());
    assertTrue(received.isEmpty());
  }

  @Test
  void rejectsNull() {
    var publisher = new SpringDomainEventPublisher(spring);
    assertThrows(IllegalArgumentException.class, () -> publisher.publish((DomainEvent) null));
    assertThrows(IllegalArgumentException.class, () -> publisher.publishAndClearEvents(null));
    assertThrows(IllegalArgumentException.class, () -> new SpringDomainEventPublisher(null));
  }
}
