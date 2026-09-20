package dev.domaincentric.dca.archunit.fixtures.errors.good.reservation.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainException;
import java.io.Serial;

public final class ReservationAlreadyConfirmedException extends DomainException {

  @Serial private static final long serialVersionUID = 1L;

  public ReservationAlreadyConfirmedException(ReservationId id) {
    super("reservation " + id.value() + " is already confirmed");
  }
}
