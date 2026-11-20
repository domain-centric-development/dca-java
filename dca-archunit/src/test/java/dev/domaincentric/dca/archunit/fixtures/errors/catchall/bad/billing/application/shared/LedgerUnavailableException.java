package dev.domaincentric.dca.archunit.fixtures.errors.catchall.bad.billing.application.shared;

import dev.domaincentric.dca.buildingblocks.application.OutputPortException;
import java.io.Serial;

/** Declared by the ledger port. */
public final class LedgerUnavailableException extends OutputPortException {

  @Serial private static final long serialVersionUID = 1L;

  public LedgerUnavailableException(String invoiceId) {
    super("Ledger refused " + invoiceId);
  }
}
