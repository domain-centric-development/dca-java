package dev.domaincentric.dca.archunit.fixtures.frameworks.jakarta.billing.application.issueinvoice;

import dev.domaincentric.dca.archunit.fixtures.frameworks.jakarta.billing.application.shared.InvoiceRepository;
import dev.domaincentric.dca.archunit.fixtures.frameworks.jakarta.billing.domain.model.Invoice;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
@Transactional
public class IssueInvoiceUseCase implements IssueInvoiceInputPort {
  private final InvoiceRepository invoices;
  private final DomainEventPublisher events;

  public IssueInvoiceUseCase(InvoiceRepository invoices, DomainEventPublisher events) {
    this.invoices = invoices;
    this.events = events;
  }

  @Override
  public IssueInvoiceResult execute(IssueInvoiceCommand command) {
    Invoice invoice = Invoice.issue();
    invoices.save(invoice);
    events.publishAndClearEvents(invoice);
    return IssueInvoiceResult.from(invoice);
  }
}
