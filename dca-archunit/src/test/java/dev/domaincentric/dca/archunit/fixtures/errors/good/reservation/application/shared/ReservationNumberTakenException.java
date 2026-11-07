package dev.domaincentric.dca.archunit.fixtures.errors.good.reservation.application.shared;

import dev.domaincentric.dca.buildingblocks.application.OutputPortException;
import java.io.Serial;

/**
 * Declared by the repository port: a reservation number only the store can see is taken. The
 * adapter raises it, the use case catches it.
 */
public final class ReservationNumberTakenException extends OutputPortException {

  @Serial private static final long serialVersionUID = 1L;

  public ReservationNumberTakenException(String number) {
    super("Reservation number " + number + " is taken");
  }
}
