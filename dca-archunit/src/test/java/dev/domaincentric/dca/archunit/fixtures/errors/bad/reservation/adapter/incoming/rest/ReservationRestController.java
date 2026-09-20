package dev.domaincentric.dca.archunit.fixtures.errors.bad.reservation.adapter.incoming.rest;

import dev.domaincentric.dca.archunit.fixtures.errors.bad.reservation.application.confirmreservation.ConfirmReservationCommand;
import dev.domaincentric.dca.archunit.fixtures.errors.bad.reservation.application.confirmreservation.ConfirmReservationInputPort;

public final class ReservationRestController {
  private final ConfirmReservationInputPort confirmReservation;

  public ReservationRestController(ConfirmReservationInputPort confirmReservation) {
    this.confirmReservation = confirmReservation;
  }

  public String confirm(String reservationId) {
    try {
      return confirmReservation
          .execute(new ConfirmReservationCommand(reservationId))
          .reservationId();
    } catch (RuntimeException anything) {
      return "500";
    }
  }
}
