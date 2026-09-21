package dev.domaincentric.dca.archunit.fixtures.owntimestamp.vocabulary;

import java.time.Instant;

/** The code base wraps its occurrence time in a value object, as DCA teaches for primitives. */
public record Timestamp(Instant value) {
  public static Timestamp now() {
    return new Timestamp(Instant.now());
  }
}
