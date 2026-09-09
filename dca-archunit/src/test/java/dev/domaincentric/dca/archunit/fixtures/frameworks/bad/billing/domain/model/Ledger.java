package dev.domaincentric.dca.archunit.fixtures.frameworks.bad.billing.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;
import io.micronaut.data.annotation.MappedEntity;

@MappedEntity
public class Ledger extends BaseAggregateRoot<Ledger, InvoiceId> {
  private final InvoiceId id;

  public Ledger(InvoiceId id) {
    this.id = id;
  }

  @Override
  public InvoiceId id() {
    return id;
  }
}
