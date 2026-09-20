package dev.domaincentric.dca.archunit.fixtures.errors.bad.reservation.application.confirmreservation;

import dev.domaincentric.dca.archunit.fixtures.errors.bad.reservation.application.shared.ReservationRepository;
import dev.domaincentric.dca.archunit.fixtures.errors.bad.reservation.domain.model.Reservation;
import dev.domaincentric.dca.archunit.fixtures.errors.bad.reservation.domain.model.ReservationId;
import org.springframework.stereotype.Service;

@Service
public final class ConfirmReservationUseCase implements ConfirmReservationInputPort {
  private final ReservationRepository reservations;

  public ConfirmReservationUseCase(ReservationRepository reservations) {
    this.reservations = reservations;
  }

  @Override
  public ConfirmReservationResult execute(ConfirmReservationCommand command) {
    ReservationId id = new ReservationId(command.reservationId());
    Reservation reservation =
        reservations
            .findById(id)
            .orElseThrow(() -> new ReservationNotFoundException(command.reservationId()));
    reservation.confirm();
    reservations.save(reservation);
    return new ConfirmReservationResult(id.value());
  }
}
