package dev.domaincentric.dca.archunit.fixtures.frameworks.bad.billing.adapter.incoming;

import dev.domaincentric.dca.archunit.fixtures.frameworks.bad.billing.application.issueinvoice.IssueInvoiceCommand;
import dev.domaincentric.dca.archunit.fixtures.frameworks.bad.billing.application.issueinvoice.IssueInvoiceInputPort;
import jakarta.ws.rs.Path;

/** A JAX-RS resource without the REST-controller suffix - visible only to the Jakarta preset. */
@Path("/invoices")
public class InvoiceEndpoint {
  private final IssueInvoiceInputPort issueInvoice;

  public InvoiceEndpoint(IssueInvoiceInputPort issueInvoice) {
    this.issueInvoice = issueInvoice;
  }

  public String issue(String customerReference) {
    return issueInvoice.execute(new IssueInvoiceCommand(customerReference)).invoiceId().toString();
  }
}
