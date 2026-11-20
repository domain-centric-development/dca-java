package dev.domaincentric.dca.archunit.fixtures.errors.catchall.bad.billing.application.shared;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.OutputPort;

/** An output port. */
public interface InvoiceLedger extends OutputPort {
  void record(String invoiceId);
}
