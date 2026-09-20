package dev.domaincentric.dca.archunit.fixtures.errors.bad.reservation.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainException;
import java.io.Serial;
import org.springframework.stereotype.Service;

@Service
public final class ReservationOverbookedException extends DomainException {

  @Serial private static final long serialVersionUID = 1L;

  public ReservationOverbookedException(ReservationId id) {
    super("reservation " + id.value() + " exceeds the capacity");
  }
}
