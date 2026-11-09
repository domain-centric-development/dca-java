package dev.domaincentric.dca.archunit.fixtures.errors.good.reservation.application.cancelreservation;

import dev.domaincentric.dca.buildingblocks.application.Refusal;

/** A closed result: one success case, the expected refusals marked as such. */
public sealed interface CancelReservationResult
    permits CancelReservationResult.Cancelled, CancelReservationResult.Refused {

  record Cancelled(String reservationId) implements CancelReservationResult {}

  /** Refusals may be grouped below a sealed case of their own. */
  sealed interface Refused extends CancelReservationResult
      permits NotFound, AlreadyStarted {}

  record NotFound(String reservationId) implements Refused, Refusal {}

  record AlreadyStarted(String reservationId) implements Refused, Refusal {}
}
