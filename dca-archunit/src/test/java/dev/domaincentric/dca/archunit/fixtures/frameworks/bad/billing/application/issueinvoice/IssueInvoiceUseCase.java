package dev.domaincentric.dca.archunit.fixtures.frameworks.bad.billing.application.issueinvoice;

import java.util.UUID;

/** No stereotype at all - fails the injectable rule under every preset that has one. */
public class IssueInvoiceUseCase implements IssueInvoiceInputPort {
  @Override
  public IssueInvoiceResult execute(IssueInvoiceCommand command) {
    return new IssueInvoiceResult(UUID.randomUUID());
  }
}
