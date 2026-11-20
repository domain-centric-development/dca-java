package dev.domaincentric.dca.archunit.fixtures.errors.catchall.good.billing.application.payinvoice;

import dev.domaincentric.dca.archunit.fixtures.errors.catchall.good.billing.application.shared.InvoiceLedger;
import dev.domaincentric.dca.archunit.fixtures.errors.catchall.good.billing.application.shared.LedgerUnavailableException;

/** Catches the failure its output port declares, by its own type. */
public final class PayInvoiceUseCase implements PayInvoiceInputPort {
  private final InvoiceLedger ledger;

  public PayInvoiceUseCase(InvoiceLedger ledger) {
    this.ledger = ledger;
  }

  @Override
  public PayInvoiceResult execute(PayInvoiceCommand command) {
    try {
      ledger.record(command.invoiceId());
    } catch (LedgerUnavailableException refused) {
      throw new InvoiceNotPayableException(command.invoiceId());
    }
    return new PayInvoiceResult(command.invoiceId());
  }
}
