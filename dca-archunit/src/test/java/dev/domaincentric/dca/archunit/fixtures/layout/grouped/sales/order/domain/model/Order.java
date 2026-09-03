package dev.domaincentric.dca.archunit.fixtures.layout.grouped.sales.order.domain.model;

import dev.domaincentric.dca.archunit.fixtures.layout.grouped.infrastructure.config.GroupedConfig;

/** Violates DCA-LAY-002 from a context nested two segments below the base package. */
public record Order(String id) {
  public int capacity() {
    return GroupedConfig.capacity();
  }
}
