package dev.domaincentric.dca.archunit.fixtures.frameworks.jakarta.billing.application.issueinvoice;

import dev.domaincentric.dca.archunit.fixtures.frameworks.jakarta.billing.domain.model.Invoice;
import java.util.UUID;

public record IssueInvoiceResult(UUID invoiceId) {
  public static IssueInvoiceResult from(Invoice invoice) {
    return new IssueInvoiceResult(invoice.id().value());
  }
}
