package dev.domaincentric.dca.archunit.fixtures.errors.bad.reservation.adapter.outgoing;

import dev.domaincentric.dca.buildingblocks.application.OutputPortException;
import java.io.Serial;

/** Invented by the adapter instead of declared beside the port. */
public final class StoreRefusedReservationException extends OutputPortException {

  @Serial private static final long serialVersionUID = 1L;

  public StoreRefusedReservationException(String id) {
    super("Store refused reservation " + id);
  }
}
