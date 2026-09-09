package dev.domaincentric.dca.archunit.fixtures.frameworks.bad.billing.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.Entity;

@ApplicationScoped
@Entity
public class Invoice extends BaseAggregateRoot<Invoice, InvoiceId> {
  private final InvoiceId id;

  public Invoice(InvoiceId id) {
    this.id = id;
  }

  @Override
  public InvoiceId id() {
    return id;
  }
}
