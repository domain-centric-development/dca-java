package dev.domaincentric.dca.archunit.fixtures.errors.good.reservation.adapter.incoming.rest;

import dev.domaincentric.dca.archunit.fixtures.errors.good.reservation.application.confirmreservation.ConfirmReservationCommand;
import dev.domaincentric.dca.archunit.fixtures.errors.good.reservation.application.confirmreservation.ConfirmReservationInputPort;
import dev.domaincentric.dca.archunit.fixtures.errors.good.reservation.application.confirmreservation.ReservationNotFoundException;
import dev.domaincentric.dca.archunit.fixtures.errors.good.reservation.domain.model.ReservationAlreadyConfirmedException;

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
    } catch (ReservationNotFoundException notFound) {
      return "404";
    } catch (ReservationAlreadyConfirmedException conflict) {
      return "409";
    }
  }
}
