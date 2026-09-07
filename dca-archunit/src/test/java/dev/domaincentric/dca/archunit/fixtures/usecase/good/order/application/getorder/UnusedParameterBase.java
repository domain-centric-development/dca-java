package dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.getorder;

/** Declares a type parameter that no instance field uses. */
public abstract class UnusedParameterBase<T> {
  protected final int count;

  protected UnusedParameterBase(int count) {
    this.count = count;
  }
}
