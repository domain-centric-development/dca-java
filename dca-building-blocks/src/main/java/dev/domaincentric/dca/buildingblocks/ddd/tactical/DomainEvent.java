package dev.domaincentric.dca.buildingblocks.ddd.tactical;

import java.time.Instant;
import java.util.UUID;

/**
 * Interface for Domain Events.
 *
 * <p>Domain Events represent something that happened in the domain that domain experts care about.
 * They are internal to a bounded context: no published contract and no version number. Once an
 * outbox or event publication registry stores them for a transactional listener, the next
 * deployment reads what the previous one wrote, so a change follows what a database migration needs
 * — tolerant reading, renames in two steps.
 *
 * <p><b>Characteristics:</b>
 *
 * <ul>
 *   <li>Immutable (final classes or records)
 *   <li>Named in the past tense (e.g., ContractSigned, ShipmentDispatched, PriceChanged)
 *   <li>Include timestamp, unique ID, and event-specific data
 *   <li>Carries no container stereotype and no listener annotation - it is data, not a bean
 *   <li>Part of the Ubiquitous Language
 *   <li>Internal to a bounded context — no contract, but readable across one deployment
 * </ul>
 *
 * <p><b>Use Cases:</b>
 *
 * <ul>
 *   <li>Triggering side effects in other aggregates or contexts
 *   <li>Enabling eventual consistency between bounded contexts
 *   <li>Audit trail and event sourcing
 *   <li>Decoupling bounded contexts
 * </ul>
 *
 * <p><b>Required Methods:</b>
 *
 * <ul>
 *   <li>{@code eventId()} - Unique identifier for this event instance
 *   <li>{@code occurredOn()} - When the event occurred
 * </ul>
 *
 * <p>For events that cross bounded context boundaries, see {@link IntegrationEvent} which adds
 * versioning for backward compatibility.
 *
 * <p><b>Example:</b>
 *
 * <pre>
 * public record ProductCreated(
 *     UUID eventId,
 *     ProductId productId,
 *     Instant occurredOn) implements DomainEvent {
 *
 *   public static ProductCreated now(ProductId productId) {
 *     return new ProductCreated(UUID.randomUUID(), productId, Instant.now());
 *   }
 * }
 * </pre>
 *
 * <p><b>References:</b>
 *
 * <ul>
 *   <li>Eric Evans' Domain-Driven Design (2003)
 *   <li>Vaughn Vernon's Implementing Domain-Driven Design (2013), Chapter 8: "Domain Events"
 * </ul>
 *
 * @see <a href="https://www.domainlanguage.com/ddd/">Domain-Driven Design Reference</a>
 */
public interface DomainEvent {

  /**
   * Unique identifier for this event instance.
   *
   * @return the event ID
   */
  UUID eventId();

  /**
   * Timestamp when the event occurred in the domain.
   *
   * @return the occurrence timestamp
   */
  Instant occurredOn();
}
