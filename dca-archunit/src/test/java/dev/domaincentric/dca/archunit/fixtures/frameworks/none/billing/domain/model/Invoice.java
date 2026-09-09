package dev.domaincentric.dca.archunit.fixtures.frameworks.none.billing.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;
import java.time.Instant;
import java.util.UUID;

public class Invoice extends BaseAggregateRoot<Invoice, InvoiceId> {
  private final InvoiceId id;

  private Invoice(InvoiceId id) {
    this.id = id;
  }

  public static Invoice issue() {
    Invoice invoice = new Invoice(new InvoiceId(UUID.randomUUID()));
    invoice.registerEvent(new InvoiceIssued(UUID.randomUUID(), Instant.now(), invoice.id));
    return invoice;
  }

  @Override
  public InvoiceId id() {
    return id;
  }
}
