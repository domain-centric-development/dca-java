package dev.domaincentric.dca.archunit.fixtures.errors.good.reservation.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainException;
import java.io.Serial;

/**
 * Status is Ubiquitous Language here, not a transport concept: DCA-ERR-005 matches the compound
 * transport words, not the bare word, so this name passes.
 */
public final class ReservationStatusInvalidException extends DomainException {

  @Serial private static final long serialVersionUID = 1L;

  public ReservationStatusInvalidException(ReservationId id) {
    super("reservation " + id.value() + " is in a status that does not allow this");
  }
}
