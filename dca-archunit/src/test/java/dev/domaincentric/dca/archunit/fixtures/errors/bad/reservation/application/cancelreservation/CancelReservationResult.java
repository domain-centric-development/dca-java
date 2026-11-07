package dev.domaincentric.dca.archunit.fixtures.errors.bad.reservation.application.cancelreservation;

/**
 * A closed result whose refusing case is not marked: its caller cannot tell, nor can the boundary.
 */
public sealed interface CancelReservationResult
    permits CancelReservationResult.Cancelled, CancelReservationResult.NotFound {

  record Cancelled(String reservationId) implements CancelReservationResult {}

  record NotFound(String reservationId) implements CancelReservationResult {}
}
