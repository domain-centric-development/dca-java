package dev.domaincentric.dca.archunit.fixtures.errors.bad.reservation.application.confirmreservation;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainException;
import java.io.Serial;

/**
 * A failure of the model, declared in the application layer. DCA-ERR-002 owns this case and says
 * what to do: move it to the domain. DCA-ERR-003 must not also report it — its remedy, "extend
 * UseCaseException", would turn a broken business rule into a use-case failure.
 */
public final class ReservationRuleBrokenException extends DomainException {

  @Serial private static final long serialVersionUID = 1L;

  public ReservationRuleBrokenException(String reservationId) {
    super("reservation " + reservationId + " breaks a rule of the model");
  }
}
