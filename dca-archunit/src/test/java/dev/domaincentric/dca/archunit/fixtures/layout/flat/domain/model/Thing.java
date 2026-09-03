package dev.domaincentric.dca.archunit.fixtures.layout.flat.domain.model;

import dev.domaincentric.dca.archunit.fixtures.layout.flat.infrastructure.config.FlatConfig;

/** Violates DCA-LAY-002 in a flat single-context layout. */
public record Thing(String id) {
  public int capacity() {
    return FlatConfig.capacity();
  }
}
