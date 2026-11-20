package dev.domaincentric.dca.archunit.fixtures.errors.catchall.bad.billing.application.payinvoice;

import dev.domaincentric.dca.archunit.fixtures.errors.catchall.bad.billing.application.shared.InvoiceLedger;

/** Catches every failure around its output port: DCA-ERR-010. */
public final class PayInvoiceUseCase implements PayInvoiceInputPort {
  private final InvoiceLedger ledger;

  public PayInvoiceUseCase(InvoiceLedger ledger) {
    this.ledger = ledger;
  }

  @Override
  public PayInvoiceResult execute(PayInvoiceCommand command) {
    try {
      ledger.record(command.invoiceId());
    } catch (RuntimeException anything) {
      throw new InvoiceNotPayableException(command.invoiceId());
    }
    return new PayInvoiceResult(command.invoiceId());
  }
}
