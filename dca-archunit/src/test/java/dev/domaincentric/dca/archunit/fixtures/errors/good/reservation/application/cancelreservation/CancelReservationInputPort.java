package dev.domaincentric.dca.archunit.fixtures.errors.good.reservation.application.cancelreservation;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.InputPort;

public interface CancelReservationInputPort extends InputPort {
  CancelReservationResult cancel(CancelReservationCommand command);
}
