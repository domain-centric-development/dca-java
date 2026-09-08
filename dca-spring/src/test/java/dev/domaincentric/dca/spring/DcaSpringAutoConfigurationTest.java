package dev.domaincentric.dca.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.domaincentric.dca.buildingblocks.application.TransactionBoundary;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

class DcaSpringAutoConfigurationTest {

  private final ApplicationContextRunner runner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(DcaSpringAutoConfiguration.class));

  @Configuration
  static class WithManager {
    @Bean
    PlatformTransactionManager transactionManager() {
      return new RecordingTransactionManager();
    }
  }

  @Configuration
  static class OwnPorts {
    @Bean
    DomainEventPublisher ownPublisher() {
      return new SpringDomainEventPublisher(event -> {});
    }

    @Bean
    TransactionBoundary ownBoundary() {
      return new InMemoryTransactionBoundary();
    }
  }

  @Test
  void registersBothAdaptersWhenATransactionManagerExists() {
    runner
        .withUserConfiguration(WithManager.class)
        .run(
            ctx -> {
              assertTrue(
                  ctx.getBean(DomainEventPublisher.class) instanceof SpringDomainEventPublisher);
              assertTrue(
                  ctx.getBean(TransactionBoundary.class) instanceof SpringTransactionBoundary);
            });
  }

  @Test
  void registersOnlyThePublisherWithoutATransactionManager() {
    runner.run(
        ctx -> {
          assertEquals(1, ctx.getBeansOfType(DomainEventPublisher.class).size());
          assertFalse(ctx.containsBean("dcaTransactionBoundary"), "no manager, no boundary");
        });
  }

  @Test
  void registersNothingWhenSwitchedOff() {
    runner
        .withUserConfiguration(WithManager.class)
        .withPropertyValues("dca.spring.enabled=false")
        .run(
            ctx -> {
              assertTrue(ctx.getBeansOfType(DomainEventPublisher.class).isEmpty());
              assertTrue(ctx.getBeansOfType(TransactionBoundary.class).isEmpty());
            });
  }

  @Test
  void backsOffWhenTheApplicationDefinesThePortsItself() {
    runner
        .withUserConfiguration(WithManager.class, OwnPorts.class)
        .run(
            ctx -> {
              assertSame(ctx.getBean("ownPublisher"), ctx.getBean(DomainEventPublisher.class));
              assertSame(ctx.getBean("ownBoundary"), ctx.getBean(TransactionBoundary.class));
            });
  }
}
