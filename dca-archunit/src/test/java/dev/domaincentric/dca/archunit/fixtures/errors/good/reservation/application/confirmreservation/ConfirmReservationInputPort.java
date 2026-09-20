package dev.domaincentric.dca.archunit.fixtures.errors.good.reservation.application.confirmreservation;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface ConfirmReservationInputPort
    extends UseCase<ConfirmReservationCommand, ConfirmReservationResult> {}
