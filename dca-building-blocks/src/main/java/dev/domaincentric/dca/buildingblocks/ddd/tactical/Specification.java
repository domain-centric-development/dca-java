package dev.domaincentric.dca.buildingblocks.ddd.tactical;

/**
 * Marker interface for Specifications.
 *
 * <p>Specifications express business rules as first-class objects. They test whether an object
 * satisfies certain criteria and can be combined to create complex business rules.
 *
 * <p><b>Use Cases:</b>
 *
 * <ul>
 *   <li>Validating whether an object meets certain criteria
 *   <li>Selecting objects from a collection
 *   <li>Specifying how to create objects that fulfill requirements
 * </ul>
 *
 * <p><b>Characteristics:</b>
 *
 * <ul>
 *   <li>Immutable value objects
 *   <li>Combinable (AND, OR, NOT operations)
 *   <li>Express business rules in the Ubiquitous Language
 *   <li>Should NOT have Spring annotations
 *   <li>Part of the domain model
 * </ul>
 *
 * <p><b>Example:</b>
 *
 * <pre>{@code
 * public final class ProductAvailabilitySpecification implements Specification<Product> {
 *   @Override
 *   public boolean isSatisfiedBy(Product product) {
 *     return product.isAvailable();
 *   }
 *
 *   public Specification<Product> and(Specification<Product> other) {
 *     return candidate -> isSatisfiedBy(candidate) && other.isSatisfiedBy(candidate);
 *   }
 * }
 * }</pre>
 *
 * <p><b>Pattern:</b> the interface is generic in the candidate type; {@link #isSatisfiedBy(Object)}
 * evaluates the specification for one candidate.
 *
 * <p><b>References:</b>
 *
 * <ul>
 *   <li>Eric Evans' Domain-Driven Design (2003), Chapter 9: "Specification"
 *   <li>Martin Fowler's <a href="https://martinfowler.com/apsupp/spec.pdf">Specifications
 *       Pattern</a>
 * </ul>
 *
 * @see <a href="https://www.domainlanguage.com/ddd/">Domain-Driven Design Reference</a>
 */
public interface Specification<T> {
  /** Whether the candidate satisfies this specification. */
  boolean isSatisfiedBy(T candidate);
}
