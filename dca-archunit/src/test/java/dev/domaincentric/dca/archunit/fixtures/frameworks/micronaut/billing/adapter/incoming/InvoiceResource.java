package dev.domaincentric.dca.archunit.fixtures.frameworks.micronaut.billing.adapter.incoming;

import dev.domaincentric.dca.archunit.fixtures.frameworks.micronaut.billing.application.issueinvoice.IssueInvoiceCommand;
import dev.domaincentric.dca.archunit.fixtures.frameworks.micronaut.billing.application.issueinvoice.IssueInvoiceInputPort;
import io.micronaut.http.annotation.Controller;

@Controller("/invoices")
public class InvoiceResource {
  private final IssueInvoiceInputPort issueInvoice;

  public InvoiceResource(IssueInvoiceInputPort issueInvoice) {
    this.issueInvoice = issueInvoice;
  }

  public String issue(String customerReference) {
    return issueInvoice.execute(new IssueInvoiceCommand(customerReference)).invoiceId().toString();
  }
}
