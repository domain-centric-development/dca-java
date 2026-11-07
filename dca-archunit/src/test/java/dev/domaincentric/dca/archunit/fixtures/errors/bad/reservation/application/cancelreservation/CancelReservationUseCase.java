package dev.domaincentric.dca.archunit.fixtures.errors.bad.reservation.application.cancelreservation;

import dev.domaincentric.dca.archunit.fixtures.errors.bad.reservation.application.shared.ReservationRepository;
import dev.domaincentric.dca.archunit.fixtures.errors.bad.reservation.domain.model.ReservationId;
import dev.domaincentric.dca.archunit.fixtures.errors.bad.reservation.domain.model.ReservationUnknownException;
import org.springframework.stereotype.Service;

/** Invents a domain failure for what the repository did not find: DCA-ERR-009. */
@Service
public final class CancelReservationUseCase implements CancelReservationInputPort {
  private final ReservationRepository reservations;

  public CancelReservationUseCase(ReservationRepository reservations) {
    this.reservations = reservations;
  }

  @Override
  public CancelReservationResult cancel(CancelReservationCommand command) {
    ReservationId id = new ReservationId(command.reservationId());
    reservations.findById(id).orElseThrow(() -> new ReservationUnknownException(id));
    return new CancelReservationResult.Cancelled(id.value());
  }
}
