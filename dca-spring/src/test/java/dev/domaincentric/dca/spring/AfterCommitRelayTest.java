package dev.domaincentric.dca.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * The failure mode this artifact exists for: an after-commit listener fires only inside a
 * transaction. Through the boundary it does; without one the same publication reaches nobody, and
 * nothing says so.
 */
class AfterCommitRelayTest {

  @Configuration
  @EnableTransactionManagement
  static class Config {
    @Bean
    PlatformTransactionManager transactionManager() {
      return new RecordingTransactionManager();
    }

    @Bean
    SpringTransactionBoundary boundary(PlatformTransactionManager manager) {
      return new SpringTransactionBoundary(manager);
    }

    @Bean
    AfterCommitListener afterCommitListener() {
      return new AfterCommitListener();
    }
  }

  static class AfterCommitListener {
    final List<Fixtures.OrderPlaced> received = new ArrayList<>();

    @TransactionalEventListener
    void on(Fixtures.OrderPlaced event) {
      received.add(event);
    }
  }

  private AnnotationConfigApplicationContext context;
  private SpringDomainEventPublisher publisher;
  private AfterCommitListener listener;

  @BeforeEach
  void start() {
    context = new AnnotationConfigApplicationContext(Config.class);
    publisher = new SpringDomainEventPublisher(context);
    listener = context.getBean(AfterCommitListener.class);
  }

  @AfterEach
  void stop() {
    context.close();
  }

  @Test
  void firesAfterCommitWhenPublishedInsideTheBoundary() {
    var boundary = context.getBean(SpringTransactionBoundary.class);
    var order = new Fixtures.Order();
    order.place();

    boundary.inTransaction(
        () -> {
          publisher.publishAndClearEvents(order);
          assertTrue(listener.received.isEmpty(), "not before commit");
        });

    assertEquals(1, listener.received.size());
  }

  @Test
  void doesNotFireWhenPublishedWithoutATransaction() {
    var order = new Fixtures.Order();
    order.place();

    publisher.publishAndClearEvents(order);

    assertTrue(listener.received.isEmpty(), "skipped silently — the reason DCA-USE-012 exists");
    assertTrue(order.domainEvents().isEmpty(), "…and the events are gone nevertheless");
  }
}
