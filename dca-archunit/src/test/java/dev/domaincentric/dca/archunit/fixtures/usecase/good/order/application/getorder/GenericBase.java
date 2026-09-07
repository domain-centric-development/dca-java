package dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.getorder;

/** A generic base without the {@code Result} suffix; results below bind {@code T} to values. */
public abstract class GenericBase<T> {
  protected final T value;

  protected GenericBase(T value) {
    this.value = value;
  }
}
