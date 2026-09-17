package dev.domaincentric.dca.archunit.fixtures.tactical.statics.billing.domain.model;

import dev.domaincentric.dca.archunit.fixtures.tactical.statics.billing.application.shared.InvoiceRepository;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;

/**
 * A static repository reference is a port in the aggregate all the same (DCA-TAC-002 reports it); a
 * static template of the own aggregate type carries no aggregate state (DCA-TAC-003 does not).
 */
public final class Invoice extends BaseAggregateRoot<Invoice, InvoiceId> {
  static InvoiceRepository lookup;
  static Invoice template;

  private final InvoiceId id;

  public Invoice(InvoiceId id) {
    this.id = id;
  }

  @Override
  public InvoiceId id() {
    return id;
  }
}
