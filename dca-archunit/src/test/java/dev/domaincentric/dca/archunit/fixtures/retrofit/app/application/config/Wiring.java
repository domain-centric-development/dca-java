package dev.domaincentric.dca.archunit.fixtures.retrofit.app.application.config;

import dev.domaincentric.dca.archunit.fixtures.retrofit.app.billing.application.payinvoice.PayInvoiceApplicationService;

public final class Wiring {
  public PayInvoiceApplicationService payInvoice() {
    return new PayInvoiceApplicationService(null);
  }
}
