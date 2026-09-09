package dev.domaincentric.dca.archunit.fixtures.frameworks.none.billing.application.issueinvoice;

import dev.domaincentric.dca.archunit.fixtures.frameworks.none.billing.application.shared.InvoiceRepository;
import dev.domaincentric.dca.archunit.fixtures.frameworks.none.billing.domain.model.Invoice;
import dev.domaincentric.dca.buildingblocks.application.TransactionBoundary;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;

public class IssueInvoiceUseCase implements IssueInvoiceInputPort {
  private final InvoiceRepository invoices;
  private final DomainEventPublisher events;
  private final TransactionBoundary transactions;

  public IssueInvoiceUseCase(
      InvoiceRepository invoices, DomainEventPublisher events, TransactionBoundary transactions) {
    this.invoices = invoices;
    this.events = events;
    this.transactions = transactions;
  }

  @Override
  public IssueInvoiceResult execute(IssueInvoiceCommand command) {
    return transactions.inTransaction(
        () -> {
          Invoice invoice = Invoice.issue();
          invoices.save(invoice);
          events.publishAndClearEvents(invoice);
          return IssueInvoiceResult.from(invoice);
        });
  }
}
