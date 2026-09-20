package dev.domaincentric.dca.archunit.fixtures.errors.good.reservation.application.confirmreservation;

import dev.domaincentric.dca.buildingblocks.application.UseCaseException;
import java.io.Serial;

public final class ReservationNotFoundException extends UseCaseException {

  @Serial private static final long serialVersionUID = 1L;

  public ReservationNotFoundException(String reservationId) {
    super("no reservation " + reservationId);
  }
}
