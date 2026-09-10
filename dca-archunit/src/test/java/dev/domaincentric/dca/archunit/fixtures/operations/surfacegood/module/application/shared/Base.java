package dev.domaincentric.dca.archunit.fixtures.operations.surfacegood.module.application.shared;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.*;

public abstract class Base<T> implements UseCase<T, T> {
  public T execute(T value) {
    return value;
  }
}
