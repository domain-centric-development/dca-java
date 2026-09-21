package dev.domaincentric.dca.archunit.fixtures.errors.bad.reservation.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainException;
import java.io.Serial;

public final class ReservationHttpResponseException extends DomainException {

  @Serial private static final long serialVersionUID = 1L;

  public ReservationHttpResponseException(ReservationId id) {
    super("reservation " + id.value() + " cannot be served");
  }
}
