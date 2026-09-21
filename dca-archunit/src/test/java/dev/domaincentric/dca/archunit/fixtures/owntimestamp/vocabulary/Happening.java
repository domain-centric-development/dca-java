package dev.domaincentric.dca.archunit.fixtures.owntimestamp.vocabulary;

/** A code base's own domain-event marker, unrelated to the library's. */
public interface Happening {
  Timestamp occurredOn();
}
