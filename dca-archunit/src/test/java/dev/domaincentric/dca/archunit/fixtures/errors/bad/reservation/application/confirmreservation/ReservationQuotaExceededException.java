package dev.domaincentric.dca.archunit.fixtures.errors.bad.reservation.application.confirmreservation;

import dev.domaincentric.dca.buildingblocks.application.InputPortException;
import java.io.Serial;

/** Extends the classification instead of UseCaseException. */
public final class ReservationQuotaExceededException extends InputPortException {

  @Serial private static final long serialVersionUID = 1L;

  public ReservationQuotaExceededException(int quota) {
    super("Reservation quota of " + quota + " exceeded");
  }
}
