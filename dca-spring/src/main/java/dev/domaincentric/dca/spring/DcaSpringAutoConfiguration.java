package dev.domaincentric.dca.spring;

import dev.domaincentric.dca.buildingblocks.application.TransactionBoundary;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Spring Boot auto-configuration: registers {@link SpringDomainEventPublisher} and — when a {@link
 * PlatformTransactionManager} exists — {@link SpringTransactionBoundary}, each only if the
 * application defines no bean of that port itself.
 *
 * <p><b>A default, not a prescription.</b> The DCA rules check that a use case publishes through
 * the {@code DomainEventPublisher} port inside a transaction boundary; which implementation stands
 * behind the port is the project's choice. Three ways to make a different one: define your own
 * {@code DomainEventPublisher} or {@code TransactionBoundary} bean (this configuration backs off
 * per port), set {@code dca.spring.enabled=false} (nothing is registered, the classes stay usable
 * by hand), or leave {@code dca-spring} off the class path altogether. {@code
 * ApplicationEventPublisher} is the default because it is what Spring's after-commit listeners and
 * Spring Modulith already build on; an integration-event publisher — outbox table, Modulith's event
 * publication registry, a broker — is deliberately not provided here, that choice is the project's.
 *
 * <p><b>Why the boundary needs a manager, and why you may have none.</b> {@code
 * spring-boot-starter} and {@code spring-modulith-starter-core} bring neither a transaction manager
 * nor {@code spring-boot-transaction} (Boot's {@code TransactionAutoConfiguration}). In that
 * configuration {@code @Transactional} compiles and does nothing: no proxy is created, no
 * transaction is opened, and every {@code @TransactionalEventListener} /
 * {@code @ApplicationModuleListener} is skipped without a log line. The JDBC and JPA starters bring
 * both. An in-memory application that wants after-commit relays needs, deliberately and visibly:
 *
 * <ol>
 *   <li>{@code org.springframework.boot:spring-boot-transaction} on the class path;
 *   <li>a {@code PlatformTransactionManager} bean — until a database arrives, a small no-op manager
 *       written in the project, so that its replacement point is visible;
 *   <li>{@code org.springframework.modulith:spring-modulith-events-api} for
 *       {@code @ApplicationModuleListener} itself (the annotation is not in {@code starter-core}).
 * </ol>
 *
 * <p>This configuration ships no no-op manager on purpose: a published one is a footgun that
 * survives into production.
 */
@AutoConfiguration(
    afterName = "org.springframework.boot.transaction.autoconfigure.TransactionAutoConfiguration")
@ConditionalOnProperty(name = "dca.spring.enabled", havingValue = "true", matchIfMissing = true)
public class DcaSpringAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(DomainEventPublisher.class)
  public SpringDomainEventPublisher dcaDomainEventPublisher(
      ApplicationEventPublisher eventPublisher) {
    return new SpringDomainEventPublisher(eventPublisher);
  }

  @Bean
  @ConditionalOnBean(PlatformTransactionManager.class)
  @ConditionalOnMissingBean(TransactionBoundary.class)
  public SpringTransactionBoundary dcaTransactionBoundary(
      PlatformTransactionManager transactionManager) {
    return new SpringTransactionBoundary(transactionManager);
  }
}
