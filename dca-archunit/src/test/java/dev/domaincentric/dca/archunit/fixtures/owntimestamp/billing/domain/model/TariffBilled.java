package dev.domaincentric.dca.archunit.fixtures.owntimestamp.billing.domain.model;

import dev.domaincentric.dca.archunit.fixtures.owntimestamp.vocabulary.Happening;
import dev.domaincentric.dca.archunit.fixtures.owntimestamp.vocabulary.Timestamp;
import java.util.UUID;

/** Stores its occurrence time - in the code base's own type, not in one of java.time. */
public record TariffBilled(UUID eventId, Timestamp occurredOn) implements Happening {}
