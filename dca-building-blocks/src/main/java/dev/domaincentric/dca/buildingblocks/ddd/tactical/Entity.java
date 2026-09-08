package dev.domaincentric.dca.buildingblocks.ddd.tactical;

/**
 * Marker interface for Entities.
 *
 * <p>An Entity is a domain object defined by its identity, not by its attributes: a line item stays
 * the same line item while its quantity changes, and two line items with equal attributes are still
 * two. Every Entity carries a typed identifier ({@link Id}) that is stable for its whole life.
 *
 * <p>An Entity that is not itself an Aggregate Root lives inside exactly one aggregate. It is
 * created, changed and removed only through its root, is never loaded or saved on its own, and is
 * therefore never returned from a Repository. Its identity is unique within the aggregate, not
 * necessarily across the system.
 *
 * <p><b>Characteristics:</b>
 *
 * <ul>
 *   <li>Has an identifier field returned by {@link #id()}
 *   <li>Equality means identity: {@link #sameIdentityAs(Entity)} compares identifiers only
 *   <li>Changes state through behaviour methods named in the ubiquitous language, never setters
 *   <li>Non-root Entities expose no public constructor - the Aggregate Root creates them
 *   <li>Holds no reference to an Aggregate Root; it is reached from the root, not the other way
 * </ul>
 *
 * <p><b>Example:</b>
 *
 * <pre>
 * public class LineItem implements Entity&lt;LineItem, LineItemId&gt; {
 *   private final LineItemId id;
 *   private Quantity quantity;
 *
 *   LineItem(LineItemId id, ProductId product, Quantity quantity) { ... }  // package-private
 *
 *   public LineItemId id() { return id; }
 *
 *   public void increaseBy(Quantity amount) { this.quantity = quantity.plus(amount); }
 * }
 * </pre>
 *
 * @param <T> the entity type (F-bounded, so {@link #sameIdentityAs(Entity)} takes the same type)
 * @param <ID> the identifier type
 */
public interface Entity<T extends Entity<T, ID>, ID extends Id> {

  /** The identifier this Entity is known by for its whole life. */
  ID id();

  /**
   * Whether {@code other} is the same Entity - the same identifier, whatever the attributes.
   *
   * @return {@code false} when {@code other} is {@code null}
   */
  default boolean sameIdentityAs(final T other) {
    return other != null && id().equals(other.id());
  }
}
