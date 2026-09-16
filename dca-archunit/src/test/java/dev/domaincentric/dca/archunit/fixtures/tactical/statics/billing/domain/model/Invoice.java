package dev.domaincentric.dca.archunit.fixtures.tactical.statics.billing.domain.model;

import dev.domaincentric.dca.archunit.fixtures.tactical.statics.billing.application.shared.InvoiceRepository;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;

/**
 * Static fields carry no aggregate state: a static repository reference and a static template of
 * the own aggregate type are neither an injected port (DCA-TAC-002) nor a held aggregate
 * (DCA-TAC-003).
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
