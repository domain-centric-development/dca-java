package dev.domaincentric.dca.buildingblocks.ddd.tactical;

/**
 * Marker interface for typed identifiers of Entities and Aggregate Roots.
 *
 * <p>An identifier is a Value Object whose only job is to name one Entity for its whole life.
 * Giving every Entity its own identifier type ({@code ProductId}, {@code OrderId}) instead of a
 * bare {@code UUID} or {@code String} lets the compiler reject a cart id passed where a product id
 * is expected, and lets a Repository's signature say which aggregate it manages.
 *
 * <p><b>Characteristics:</b>
 *
 * <ul>
 *   <li>Immutable, with attribute equality - a record is the natural shape
 *   <li>Validates its own wrapped value (never null, well-formed)
 *   <li>Carries no behaviour beyond identity; generation ({@code newId()}) may live on the type
 *   <li>Lives in the domain layer of the context that owns the Entity; identifiers shared across
 *       contexts belong in the shared kernel
 * </ul>
 *
 * <p><b>Example:</b>
 *
 * <pre>
 * public record ProductId(UUID value) implements Id {
 *   public ProductId {
 *     Objects.requireNonNull(value, "value");
 *   }
 *
 *   public static ProductId newId() {
 *     return new ProductId(UUID.randomUUID());
 *   }
 * }
 * </pre>
 */
public interface Id {}
