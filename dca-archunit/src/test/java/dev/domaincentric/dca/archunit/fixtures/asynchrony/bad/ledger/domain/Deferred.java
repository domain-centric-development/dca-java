package dev.domaincentric.dca.archunit.fixtures.asynchrony.bad.ledger.domain;

/** A code base's own awaitable - not one of the configured asynchronous types by default. */
public class Deferred<T> {

  private final T value;

  public Deferred(T value) {
    this.value = value;
  }

  public T value() {
    return value;
  }
}
