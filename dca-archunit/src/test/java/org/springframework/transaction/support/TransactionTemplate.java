package org.springframework.transaction.support;

import java.util.function.Supplier;

/** Test shim — mirrors the framework's programmatic transaction template by name only. */
public class TransactionTemplate {
  public <T> T execute(Supplier<T> action) {
    return action.get();
  }
}
