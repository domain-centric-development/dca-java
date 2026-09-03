package dev.domaincentric.dca.archunit.fixtures.layout.nested.contexts.todo.domain.model;

import dev.domaincentric.dca.archunit.fixtures.layout.nested.infrastructure.config.NestedConfig;

/**
 * Depends on infrastructure, which DCA-LAY-002 forbids. {@code contexts.todo} declares no
 * {@code @BoundedContext}; under the former one-segment wildcard it was no discovered context,
 * DCA-LAY-002 found no domain classes and passed anyway. Structural module discovery is what breaks
 * that silence.
 */
public record Todo(String id) {
  public int capacity() {
    return NestedConfig.capacity();
  }
}
