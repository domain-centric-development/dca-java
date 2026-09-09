package dev.domaincentric.dca.buildingblocks.hexagonal.port.out;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.AggregateRoot;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainEvent;

/**
 * Outbound port for publishing domain events.
 *
 * <p>This interface defines the contract for publishing domain events from the application layer to
 * the infrastructure layer, enabling loose coupling between aggregates and event handlers.
 *
 * <p>This is an outbound port (secondary/driven port in Hexagonal Architecture) used across all
 * bounded contexts, making it part of the Shared Kernel. The application layer depends on this
 * interface, while concrete implementations reside in the infrastructure layer, following the
 * Dependency Inversion Principle.
 *
 * <p><b>Usage Pattern:</b>
 *
 * <pre>
 * // In Application Service after saving aggregate:
 * Product product = productRepository.save(product);
 * domainEventPublisher.publishAndClearEvents(product);
 * </pre>
 *
 * <p><b>Benefits:</b>
 *
 * <ul>
 *   <li>Application layer remains framework-independent
 *   <li>Events are published only after successful persistence
 *   <li>Enables asynchronous event handling
 *   <li>Supports eventual consistency across aggregates
 *   <li>Easy to swap implementations or mock for testing
 * </ul>
 *
 * <p><b>Sequence of operations — save, dispatch, then clear.</b> The use case calls {@code
 * publishAndClearEvents} after {@code save}, inside the same transaction, so an event is never
 * dispatched for state that was not persisted. The implementation dispatches the collected events
 * first and clears the aggregate <em>afterwards</em>: clearing is the acknowledgement that every
 * listener has seen the event. A listener that throws therefore fails the use case and leaves the
 * events on the aggregate — nothing is silently lost. Clearing before dispatch would drop events on
 * the first failing listener.
 *
 * <p>Integration events derived from these domain events (by an outgoing event adapter listening
 * in-process) are recorded in a transactional outbox inside the same transaction and delivered
 * after commit, at least once; their consumers are idempotent.
 */
public interface DomainEventPublisher extends OutputPort {

  /**
   * Publishes a single domain event.
   *
   * <p>The event is handed to the underlying event infrastructure - the container's in-process
   * event bus, a message broker, or whatever the adapter wraps.
   *
   * @param event the domain event to publish
   * @throws IllegalArgumentException if event is null
   */
  void publish(DomainEvent event);

  /**
   * Publishes all domain events from an aggregate and clears them.
   *
   * <p>Call after successfully persisting the aggregate, inside the transaction. Dispatches all
   * collected events to their listeners and, once every listener returned, clears them from the
   * aggregate — the clear is the acknowledgement. If a listener throws, the exception propagates,
   * the events stay on the aggregate and the surrounding transaction rolls back.
   *
   * @param aggregate the aggregate containing domain events
   * @throws IllegalArgumentException if aggregate is null
   */
  void publishAndClearEvents(AggregateRoot<?, ?> aggregate);
}
