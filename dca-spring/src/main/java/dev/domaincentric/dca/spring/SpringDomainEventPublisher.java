package dev.domaincentric.dca.spring;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.AggregateRoot;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainEvent;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;

/**
 * {@link DomainEventPublisher} over Spring's {@link ApplicationEventPublisher}.
 *
 * <p>Domain events are dispatched in-process to Spring event listeners — {@code @EventListener} for
 * immediate reactions inside the transaction, {@code @TransactionalEventListener} or Spring
 * Modulith's {@code @ApplicationModuleListener} for reactions after commit. The outgoing event
 * adapters that translate a domain event into an integration event are such listeners.
 *
 * <p><b>Order of operations — dispatch, then clear.</b> {@link #publishAndClearEvents} dispatches
 * every collected event first and clears the aggregate <em>afterwards</em>: clearing is the
 * acknowledgement that every listener has seen the event. A listener that throws therefore fails
 * the use case and leaves the events on the aggregate, so nothing is silently lost — and the
 * surrounding transaction rolls back. Clearing before dispatch would drop events on the first
 * failing listener.
 *
 * <p><b>Transactions.</b> Dispatching happens synchronously in the caller's thread, inside whatever
 * transaction is active. After-commit listeners are only reached when a transaction <em>is</em>
 * active: without one they are skipped silently. The use case therefore runs under
 * {@code @Transactional} or inside {@link SpringTransactionBoundary#inTransaction}; the DCA rule
 * {@code DCA-USE-012} checks that every publishing use case does.
 *
 * <p>Register it as a bean — {@link DcaSpringAutoConfiguration} does so in a Spring Boot
 * application unless a {@code DomainEventPublisher} bean already exists.
 */
public class SpringDomainEventPublisher implements DomainEventPublisher {

  private static final System.Logger log =
      System.getLogger(SpringDomainEventPublisher.class.getName());

  private final ApplicationEventPublisher eventPublisher;

  public SpringDomainEventPublisher(ApplicationEventPublisher eventPublisher) {
    if (eventPublisher == null) {
      throw new IllegalArgumentException("ApplicationEventPublisher must not be null");
    }
    this.eventPublisher = eventPublisher;
  }

  @Override
  public void publish(DomainEvent event) {
    if (event == null) {
      throw new IllegalArgumentException("Domain event must not be null");
    }
    log.log(
        System.Logger.Level.DEBUG,
        () ->
            "Publishing domain event "
                + event.getClass().getSimpleName()
                + " (id "
                + event.eventId()
                + ", occurred "
                + event.occurredOn()
                + ")");
    eventPublisher.publishEvent(event);
  }

  @Override
  public void publishAndClearEvents(AggregateRoot<?, ?> aggregate) {
    if (aggregate == null) {
      throw new IllegalArgumentException("Aggregate must not be null");
    }
    List<DomainEvent> events = aggregate.domainEvents();
    if (events.isEmpty()) {
      return;
    }
    log.log(
        System.Logger.Level.DEBUG,
        () ->
            "Publishing "
                + events.size()
                + " domain event(s) from "
                + aggregate.getClass().getSimpleName());
    // Dispatch first; a throwing listener leaves the events on the aggregate.
    List.copyOf(events).forEach(this::publish);
    aggregate.clearDomainEvents();
  }
}
