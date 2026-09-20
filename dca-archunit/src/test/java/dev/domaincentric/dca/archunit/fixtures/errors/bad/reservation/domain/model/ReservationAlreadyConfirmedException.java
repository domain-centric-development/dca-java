package dev.domaincentric.dca.archunit.fixtures.errors.bad.reservation.domain.model;

import java.io.Serial;

public final class ReservationAlreadyConfirmedException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public ReservationAlreadyConfirmedException(ReservationId id) {
    super("reservation " + id.value() + " is already confirmed");
  }
}
