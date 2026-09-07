package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.listorders;

import java.util.List;

/** Binds the base's {@code T} to {@code List<U>} and leaves {@code U} to the next subclass. */
public abstract class Intermediate<U> extends GenericBase<List<U>> {
  protected Intermediate(List<U> value, int count) {
    super(value, count);
  }
}
