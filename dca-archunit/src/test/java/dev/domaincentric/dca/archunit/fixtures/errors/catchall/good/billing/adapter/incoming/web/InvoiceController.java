package dev.domaincentric.dca.archunit.fixtures.errors.catchall.good.billing.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.errors.catchall.good.billing.application.payinvoice.InvoiceNotPayableException;
import dev.domaincentric.dca.archunit.fixtures.errors.catchall.good.billing.application.payinvoice.PayInvoiceCommand;
import dev.domaincentric.dca.archunit.fixtures.errors.catchall.good.billing.application.payinvoice.PayInvoiceInputPort;
import java.io.StringReader;

public final class InvoiceController {
  private final PayInvoiceInputPort payInvoice;

  public InvoiceController(PayInvoiceInputPort payInvoice) {
    this.payInvoice = payInvoice;
  }

  /** The use case's own failure by its type; try-with-resources adds the compiler's own catch. */
  public String pay(String invoiceId) {
    try (StringReader body = new StringReader(invoiceId)) {
      return payInvoice.execute(new PayInvoiceCommand(invoiceId)).invoiceId() + body.ready();
    } catch (InvoiceNotPayableException notPayable) {
      return "409";
    } catch (java.io.IOException unreadable) {
      return "400";
    }
  }
}
