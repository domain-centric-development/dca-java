package dev.domaincentric.dca.archunit.fixtures.errors.bad.reservation.application.confirmreservation;

import dev.domaincentric.dca.buildingblocks.application.UseCaseException;
import java.io.Serial;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus
public final class ReservationWindowClosedException extends UseCaseException {

  @Serial private static final long serialVersionUID = 1L;

  public ReservationWindowClosedException(String reservationId) {
    super("the confirmation window of reservation " + reservationId + " has closed");
  }
}
