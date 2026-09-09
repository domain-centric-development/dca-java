package dev.domaincentric.dca.archunit.fixtures.frameworks.none.billing.adapter.incoming;

import dev.domaincentric.dca.archunit.fixtures.frameworks.none.billing.application.issueinvoice.IssueInvoiceCommand;
import dev.domaincentric.dca.archunit.fixtures.frameworks.none.billing.application.issueinvoice.IssueInvoiceInputPort;

public class InvoiceResource {
  private final IssueInvoiceInputPort issueInvoice;

  public InvoiceResource(IssueInvoiceInputPort issueInvoice) {
    this.issueInvoice = issueInvoice;
  }

  public String issue(String customerReference) {
    return issueInvoice.execute(new IssueInvoiceCommand(customerReference)).invoiceId().toString();
  }
}
