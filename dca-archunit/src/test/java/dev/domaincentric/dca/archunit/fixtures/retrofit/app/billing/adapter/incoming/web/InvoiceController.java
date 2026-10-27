package dev.domaincentric.dca.archunit.fixtures.retrofit.app.billing.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.retrofit.app.billing.application.payinvoice.PayInvoiceCommand;
import dev.domaincentric.dca.archunit.fixtures.retrofit.app.billing.application.payinvoice.PayInvoiceUseCase;
import dev.domaincentric.dca.archunit.fixtures.retrofit.app.billing.application.payinvoice.PayStatusResult;

public class InvoiceController {
  private final PayInvoiceUseCase payInvoice;

  public InvoiceController(PayInvoiceUseCase payInvoice) {
    this.payInvoice = payInvoice;
  }

  public String pay(String id, String secret) {
    PayStatusResult result = payInvoice.execute(new PayInvoiceCommand(id, secret));
    return result.name();
  }
}
