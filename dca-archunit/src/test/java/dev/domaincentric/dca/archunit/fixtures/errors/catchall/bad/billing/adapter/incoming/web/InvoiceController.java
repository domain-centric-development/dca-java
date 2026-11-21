package dev.domaincentric.dca.archunit.fixtures.errors.catchall.bad.billing.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.errors.catchall.bad.billing.application.payinvoice.PayInvoiceCommand;
import dev.domaincentric.dca.archunit.fixtures.errors.catchall.bad.billing.application.payinvoice.PayInvoiceInputPort;
import dev.domaincentric.dca.buildingblocks.application.InputPortException;

public final class InvoiceController {
  private final PayInvoiceInputPort payInvoice;

  public InvoiceController(PayInvoiceInputPort payInvoice) {
    this.payInvoice = payInvoice;
  }

  /** Catch-all around the input port: reported. */
  public String pay(String invoiceId) {
    try {
      return payInvoice.execute(new PayInvoiceCommand(invoiceId)).invoiceId();
    } catch (Exception anything) {
      return "500";
    }
  }

  /** The input port failures' common base: reported. */
  public String payOrConflict(String invoiceId) {
    try {
      return payInvoice.execute(new PayInvoiceCommand(invoiceId)).invoiceId();
    } catch (InputPortException anyFailure) {
      return "409";
    }
  }

  /** A catch-all around no port call: not this rule's concern. */
  public int parse(String text) {
    try {
      return Integer.parseInt(text);
    } catch (Exception notANumber) {
      return 0;
    }
  }

  /** Cleans up and wraps the failure: reported. */
  public String payOrWrap(String invoiceId) {
    try {
      return payInvoice.execute(new PayInvoiceCommand(invoiceId)).invoiceId();
    } catch (RuntimeException failure) {
      undo(invoiceId);
      throw new IllegalStateException(failure);
    }
  }

  /** Cleans up and throws something else: reported. */
  public String payOrThrowOther(String invoiceId) {
    try {
      return payInvoice.execute(new PayInvoiceCommand(invoiceId)).invoiceId();
    } catch (RuntimeException failure) {
      undo(invoiceId);
      throw new IllegalArgumentException(invoiceId);
    }
  }

  /** Rethrows on one path only, returns on the other: reported. */
  public String payOrSometimesReturn(String invoiceId) {
    try {
      return payInvoice.execute(new PayInvoiceCommand(invoiceId)).invoiceId();
    } catch (RuntimeException failure) {
      if (invoiceId.isEmpty()) {
        return "400";
      }
      throw failure;
    }
  }

  /** Cleans up and runs on after the try statement: reported. */
  public String payOrCarryOn(String invoiceId) {
    String answer = "";
    try {
      answer = payInvoice.execute(new PayInvoiceCommand(invoiceId)).invoiceId();
    } catch (RuntimeException failure) {
      undo(invoiceId);
    }
    return answer;
  }

  private void undo(String invoiceId) {
    invoiceId.length();
  }
}
