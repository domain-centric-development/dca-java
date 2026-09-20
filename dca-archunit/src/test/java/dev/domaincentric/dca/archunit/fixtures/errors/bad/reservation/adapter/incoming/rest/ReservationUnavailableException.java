package dev.domaincentric.dca.archunit.fixtures.errors.bad.reservation.adapter.incoming.rest;

import dev.domaincentric.dca.buildingblocks.application.UseCaseException;
import java.io.Serial;

public final class ReservationUnavailableException extends UseCaseException {

  @Serial private static final long serialVersionUID = 1L;

  public ReservationUnavailableException(String reservationId) {
    super("reservation " + reservationId + " cannot be served right now");
  }
}
