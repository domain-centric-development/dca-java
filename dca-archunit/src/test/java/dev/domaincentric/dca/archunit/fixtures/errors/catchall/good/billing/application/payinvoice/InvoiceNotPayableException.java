package dev.domaincentric.dca.archunit.fixtures.errors.catchall.good.billing.application.payinvoice;

import dev.domaincentric.dca.buildingblocks.application.UseCaseException;
import java.io.Serial;

/** The use case's own failure. */
public final class InvoiceNotPayableException extends UseCaseException {

  @Serial private static final long serialVersionUID = 1L;

  public InvoiceNotPayableException(String invoiceId) {
    super("Invoice " + invoiceId + " cannot be paid");
  }
}
