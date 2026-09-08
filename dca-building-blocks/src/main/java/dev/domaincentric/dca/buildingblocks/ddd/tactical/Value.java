package dev.domaincentric.dca.buildingblocks.ddd.tactical;

/**
 * Marker interface for Value Objects.
 *
 * <p>A Value Object describes a characteristic - an amount of money, an address, a quantity - and
 * has no identity: two values with the same attributes are the same value. It is immutable; a
 * change produces a new instance. Because it has no life cycle of its own, it is never loaded or
 * saved on its own - it travels inside the Entity or Aggregate Root that holds it.
 *
 * <p><b>Characteristics:</b>
 *
 * <ul>
 *   <li>Immutable: all fields final, no setters; a record is the natural shape
 *   <li>Equality by attributes, not by reference or identifier
 *   <li>Self-validating: the constructor rejects values that make no sense in the domain
 *   <li>Behaviour is side-effect free and returns new values ({@code money.add(other)})
 *   <li>Must not reference Aggregate Roots or Entities
 * </ul>
 *
 * <p><b>Example:</b>
 *
 * <pre>
 * public record Money(BigDecimal amount, Currency currency) implements Value {
 *   public Money {
 *     Objects.requireNonNull(amount, "amount");
 *     Objects.requireNonNull(currency, "currency");
 *   }
 *
 *   public Money add(Money other) {
 *     requireSameCurrency(other);
 *     return new Money(amount.add(other.amount), currency);
 *   }
 * }
 * </pre>
 *
 * <p>Identifiers are Value Objects too, but carry their own marker: {@link Id}.
 */
public interface Value {}
