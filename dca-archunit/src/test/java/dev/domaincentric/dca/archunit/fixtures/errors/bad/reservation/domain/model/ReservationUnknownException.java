package dev.domaincentric.dca.archunit.fixtures.errors.bad.reservation.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainException;
import java.io.Serial;

/** Declared in the domain, where it belongs by its base type - but only a use case can raise it. */
public final class ReservationUnknownException extends DomainException {

  @Serial private static final long serialVersionUID = 1L;

  public ReservationUnknownException(ReservationId id) {
    super("reservation " + id.value() + " does not exist");
  }
}
