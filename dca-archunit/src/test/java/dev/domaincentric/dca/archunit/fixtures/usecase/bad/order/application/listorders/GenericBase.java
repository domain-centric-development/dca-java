package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.listorders;

/**
 * A generic base without the {@code Result} suffix: a result that extends it decides what {@code T}
 * is, so the inherited field must be read in the subclass's context. {@code count} uses no type
 * parameter.
 */
public abstract class GenericBase<T> {
  protected final T value;
  protected final int count;

  protected GenericBase(T value, int count) {
    this.value = value;
    this.count = count;
  }
}
