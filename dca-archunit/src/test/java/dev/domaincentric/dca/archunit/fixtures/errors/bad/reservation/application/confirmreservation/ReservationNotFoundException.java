package dev.domaincentric.dca.archunit.fixtures.errors.bad.reservation.application.confirmreservation;

import java.io.Serial;

public final class ReservationNotFoundException extends RuntimeException {

  @Serial private static final long serialVersionUID = 1L;

  public ReservationNotFoundException(String reservationId) {
    super("no reservation " + reservationId);
  }
}
