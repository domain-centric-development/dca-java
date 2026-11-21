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

  /** A catch-all that only cleans up and rethrows the caught failure unchanged: passes. */
  public String payAndUndo(String invoiceId) {
    try {
      return payInvoice.execute(new PayInvoiceCommand(invoiceId)).invoiceId();
    } catch (Exception failure) {
      undo(invoiceId);
      throw failure;
    }
  }

  /** The same with a branch in the clean-up and the failure rethrown inside a lambda: passes. */
  public Runnable payLater(String invoiceId) {
    return () -> {
      try {
        payInvoice.execute(new PayInvoiceCommand(invoiceId));
      } catch (RuntimeException failure) {
        if (invoiceId.isEmpty()) {
          undo(invoiceId);
        }
        RuntimeException same = failure;
        throw same;
      }
    };
  }

  private void undo(String invoiceId) {
    invoiceId.length();
  }
}
